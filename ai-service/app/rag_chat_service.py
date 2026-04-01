"""
FinGenie AI Service - RAG Chat Service
"""
import logging
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional, Tuple

from app.config import Settings
from app.llm_service import GeminiLLMService
from app.persona_prompts import build_chat_system_instruction, infer_language_code_from_text
from app.vector_embedding_service import VectorEmbeddingService

logger = logging.getLogger(__name__)


class RAGChatService:
    """
    Chat service with retrieval-augmented context from pgvector.
    """
    INVALID_CONTEXT_VALUES = {"", "undefined", "null", "none", "nan", "n/a"}

    def __init__(self, settings: Settings, vector_service: VectorEmbeddingService):
        self.settings = settings
        self.vector_service = vector_service
        self.llm_service = GeminiLLMService(settings)

    async def retrieve_context(
        self,
        user_id: int,
        query: str,
        top_k: Optional[int] = None,
    ) -> List[Dict[str, Any]]:
        matches = await self.vector_service.retrieve_context(user_id, query, top_k)
        return [self._to_context_payload(match) for match in matches]

    async def chat(
        self,
        account_id: int,
        message: str,
        conversation_id: Optional[int],
        context: Optional[str],
        language_code: Optional[str] = None,
    ) -> Dict[str, Any]:
        normalized_message = (message or "").strip()
        if not normalized_message:
            raise ValueError("Message cannot be empty")
        sanitized_context, language_code, language_source = self._sanitize_context_and_language(
            context,
            language_code,
            normalized_message,
        )

        active_conversation_id = conversation_id or int(
            datetime.now(timezone.utc).timestamp() * 1000
        )

        retrieved_context = await self.retrieve_context(
            account_id,
            normalized_message,
            self.settings.vector_top_k,
        )
        prompt = self._build_prompt(
            normalized_message,
            sanitized_context,
            retrieved_context,
            language_code,
        )
        ai_reply = await self._call_llm(
            prompt,
            normalized_message,
            retrieved_context,
            language_code,
            language_source,
        )
        detected_intent = self._detect_intent(normalized_message)

        timestamp = datetime.now(timezone.utc).isoformat()
        user_message_payload = {
            "id": None,
            "sender": "USER",
            "text": normalized_message,
            "confidence": 1.0,
            "intent": "USER_INPUT",
            "modelUsed": "none",
            "tokenCount": self._estimate_tokens(normalized_message),
            "createdAt": timestamp,
        }
        ai_message_payload = {
            "id": None,
            "sender": "AI",
            "text": ai_reply,
            "confidence": 0.75,
            "intent": detected_intent,
            "modelUsed": self.settings.gemini_model,
            "tokenCount": self._estimate_tokens(ai_reply),
            "createdAt": timestamp,
        }

        await self.vector_service.index_text(
            account_id,
            "chat_user_message",
            active_conversation_id,
            normalized_message,
            {
                "role": "user",
                "conversation_id": active_conversation_id,
                "intent": detected_intent,
            },
        )
        await self.vector_service.index_text(
            account_id,
            "chat_ai_message",
            active_conversation_id,
            ai_reply,
            {
                "role": "assistant",
                "conversation_id": active_conversation_id,
                "intent": detected_intent,
            },
        )

        return {
            "conversationId": active_conversation_id,
            "userMessage": user_message_payload,
            "aiMessage": ai_message_payload,
            "suggestions": self._build_suggestions(detected_intent, language_code),
            "detectedIntent": detected_intent,
            "retrievedContext": retrieved_context,
        }

    async def _call_llm(
        self,
        prompt: str,
        message: str,
        retrieved_context: List[Dict[str, Any]],
        language_code: str,
        language_source: str,
    ) -> str:
        system_instruction = build_chat_system_instruction(language_code, language_source)
        text = await self.llm_service.try_generate_text(
            prompt,
            system_instruction=system_instruction,
        )
        if text:
            return text

        logger.warning("LLM call failed or unavailable, using fallback response")
        return self._fallback_response(message, retrieved_context, language_code)

    def _build_prompt(
        self,
        message: str,
        direct_context: Optional[str],
        retrieved_context: List[Dict[str, Any]],
        language_code: str,
    ) -> str:
        language_name = "Vietnamese" if language_code == "vi" else "English"
        context_lines = []
        if direct_context:
            context_lines.append(f"User context: {direct_context}")

        if retrieved_context:
            context_lines.append("Retrieved financial context (ranked):")
            for idx, item in enumerate(retrieved_context[:5], start=1):
                score = item.get("score", 0.0)
                source_type = item.get("sourceType", "context")
                raw_text = str(item.get("content", "")).strip()
                text = " ".join(raw_text.split())
                if len(text) > 240:
                    text = f"{text[:237]}..."
                if not text:
                    continue
                context_lines.append(f"{idx}. [{source_type}] (score={score:.3f}) {text}")

        context_block = "\n".join(context_lines) if context_lines else "No prior context."
        return (
            f"You must reply strictly in {language_name}.\n"
            "Only use retrieved context when relevant to the user message.\n"
            "If context is limited, state that briefly instead of inventing facts.\n"
            "If context conflicts, prefer the latest user message.\n\n"
            f"{context_block}\n\n"
            f"User message: {message}\n"
            "Assistant:"
        )

    def _fallback_response(
        self,
        message: str,
        retrieved_context: List[Dict[str, Any]],
        language_code: str,
    ) -> str:
        if language_code == "vi":
            if retrieved_context:
                top = retrieved_context[0].get("content", "")
                return (
                    "Hien tai toi khong ket noi duoc den mo hinh ngon ngu. "
                    f"Dua tren boi canh gan day, ban co the bat dau voi hanh dong nay: {top}"
                )
            return (
                "Hien tai toi khong ket noi duoc den mo hinh ngon ngu. "
                f"Hay ghi lai giao dich nay va xem lai sau: {message}"
            )

        if retrieved_context:
            top = retrieved_context[0].get("content", "")
            return (
                "I could not reach the language model right now. "
                f"Based on your recent context, start with this action: {top}"
            )
        return (
            "I could not reach the language model right now. "
            f"Please track this expense and review it later: {message}"
        )

    @staticmethod
    def _estimate_tokens(text: str) -> int:
        return max(1, len(text) // 4)

    def _detect_intent(self, message: str) -> str:
        normalized = message.lower()
        if any(word in normalized for word in ("budget", "overspend", "too much")):
            return "BUDGETING"
        if any(word in normalized for word in ("save", "saving", "goal")):
            return "SAVINGS"
        if any(word in normalized for word in ("debt", "loan", "credit")):
            return "DEBT_MANAGEMENT"
        return "GENERAL_FINANCE"

    @staticmethod
    def _build_suggestions(intent: str, language_code: str) -> List[str]:
        if language_code == "vi":
            if intent == "BUDGETING":
                return [
                    "Dat gioi han chi tieu cho tung danh muc trong tuan nay",
                    "Xem lai 3 danh muc chi tieu lon nhat",
                    "Bat nhac nho theo doi chi tieu hang ngay",
                ]
            if intent == "SAVINGS":
                return [
                    "Thiet lap chuyen tien tiet kiem tu dong",
                    "Dat muc tieu tiet kiem theo tuan",
                    "Cat giam mot khoan chi khong thiet yeu hom nay",
                ]
            if intent == "DEBT_MANAGEMENT":
                return [
                    "Uu tien tra khoan no lai suat cao truoc",
                    "Len lich tra no co dinh moi tuan",
                    "Han che vay moi trong thang nay",
                ]
            return [
                "Ghi lai toan bo chi tieu trong 7 ngay",
                "So sanh chi tieu cuoi tuan va ngay thuong",
                "Dat mot muc tieu tai chinh ngan han",
            ]

        if intent == "BUDGETING":
            return [
                "Set a category cap for this week",
                "Review your top 3 spending categories",
                "Enable daily spend tracking reminders",
            ]
        if intent == "SAVINGS":
            return [
                "Create an automatic savings transfer",
                "Set a weekly savings target",
                "Reduce one non-essential expense today",
            ]
        if intent == "DEBT_MANAGEMENT":
            return [
                "Prioritize highest-interest debt first",
                "Schedule a fixed weekly repayment",
                "Avoid new debt this month",
            ]
        return [
            "Log all expenses for 7 days",
            "Review weekend vs weekday spending",
            "Define a short-term money goal",
        ]

    def _sanitize_context_and_language(
        self,
        direct_context: Optional[str],
        preferred_language: Optional[str] = None,
        latest_message: str = "",
    ) -> Tuple[str, str, str]:
        inferred_language = infer_language_code_from_text(latest_message, default="en")
        initial_language = self._normalize_language_code(preferred_language) or inferred_language
        language_source = "structured_language_field" if self._normalize_language_code(preferred_language) else "message_inference"

        if not direct_context:
            return "", initial_language, language_source

        explicit_language = preferred_language is not None and str(preferred_language).strip() != ""
        language_code = initial_language
        cleaned_lines: List[str] = []
        seen = set()

        for raw_line in direct_context.splitlines():
            line = (raw_line or "").strip()
            if not line:
                continue

            if "=" not in line:
                if self._is_invalid_context_value(line):
                    continue
                if line not in seen:
                    seen.add(line)
                    cleaned_lines.append(line)
                continue

            key, raw_value = line.split("=", 1)
            key = key.strip()
            value = self._normalize_context_value(raw_value)
            if not key or not value:
                continue

            normalized_key = key.lower()
            if normalized_key in ("app_language", "preferred_response_language") and not explicit_language:
                normalized_from_context = self._normalize_language_code(value)
                if normalized_from_context:
                    language_code = normalized_from_context
                    language_source = f"context_{normalized_key}"

            sanitized_line = f"{key}={value}"
            if sanitized_line not in seen:
                seen.add(sanitized_line)
                cleaned_lines.append(sanitized_line)

        language_name = "Vietnamese" if language_code == "vi" else "English"
        for line in (
            f"app_language={language_code}",
            f"preferred_response_language={language_name}",
            f"response_language_strict=Reply only in {language_name}",
        ):
            if line not in seen:
                cleaned_lines.append(line)

        return "\n".join(cleaned_lines), language_code, language_source

    def _normalize_context_value(self, value: str) -> str:
        normalized = (value or "").strip().strip('"').strip("'").strip()
        if self._is_invalid_context_value(normalized):
            return ""
        return normalized

    def _is_invalid_context_value(self, value: str) -> bool:
        normalized = (value or "").strip().lower()
        return normalized in self.INVALID_CONTEXT_VALUES

    @staticmethod
    def _normalize_language_code(value: Optional[str]) -> Optional[str]:
        normalized = (value or "").strip().lower()
        if not normalized:
            return None
        if normalized.startswith("vi") or "vietnam" in normalized:
            return "vi"
        if normalized.startswith("en") or "english" in normalized:
            return "en"
        return "en"

    @staticmethod
    def _to_context_payload(match) -> Dict[str, Any]:
        return {
            "sourceType": match.source_type,
            "sourceId": match.source_id,
            "content": match.content,
            "score": match.score,
            "metadata": match.metadata,
            "createdAt": match.created_at,
        }
