"""
Shared Gemini LLM service for ai-service runtime.
"""
import asyncio
import logging
import time
from typing import Any, Optional

from app.config import Settings

try:
    from google import genai
except ModuleNotFoundError:  # pragma: no cover - runtime environment dependent
    genai = None

logger = logging.getLogger(__name__)


class GeminiLLMService:
    """Thin shared wrapper around Gemini with timeout + cooldown safety."""

    def __init__(self, settings: Settings):
        self.settings = settings
        self._last_failure_at = 0.0
        self._client: Any = None

        if not self.settings.gemini_api_key:
            logger.info("Gemini API key is not configured; LLM calls will use fallback responses")
            return
        if genai is None:
            logger.warning("google-genai package is unavailable; LLM calls will use fallback responses")
            return

        try:
            self._client = genai.Client(api_key=self.settings.gemini_api_key)
        except Exception as exc:
            logger.warning("Failed to initialize Gemini client: %s", exc)

    def model_name(self) -> str:
        return self.settings.gemini_model

    async def try_generate_text(
        self,
        prompt: str,
        *,
        system_instruction: Optional[str] = None,
        model: Optional[str] = None,
    ) -> Optional[str]:
        if self._in_cooldown():
            return None
        if not self._client:
            return None

        target_model = model or self.settings.gemini_model
        kwargs = {}
        if system_instruction:
            kwargs["config"] = {"system_instruction": system_instruction}

        try:
            timeout_seconds = max(int(self.settings.llm_timeout_seconds), 1)
            response = await asyncio.wait_for(
                asyncio.to_thread(
                    self._client.models.generate_content,
                    model=target_model,
                    contents=prompt,
                    **kwargs,
                ),
                timeout=timeout_seconds,
            )
            text = self._extract_text(response)
            return text or None
        except Exception as exc:
            self._last_failure_at = time.time()
            logger.warning("Gemini call failed (cooldown enabled): %s", exc)
            return None

    async def generate_text(
        self,
        prompt: str,
        *,
        system_instruction: Optional[str] = None,
        model: Optional[str] = None,
        fallback_text: str = "",
    ) -> str:
        text = await self.try_generate_text(
            prompt,
            system_instruction=system_instruction,
            model=model,
        )
        if text:
            return text
        return fallback_text

    def _in_cooldown(self) -> bool:
        if self._last_failure_at <= 0:
            return False
        return (time.time() - self._last_failure_at) < self.settings.llm_failure_cooldown_seconds

    @staticmethod
    def _extract_text(response: Any) -> str:
        direct_text = getattr(response, "text", None)
        if isinstance(direct_text, str) and direct_text.strip():
            return direct_text.strip()

        # Compatibility path when response.text is unavailable.
        chunks = []
        candidates = getattr(response, "candidates", None) or []
        for candidate in candidates:
            content = getattr(candidate, "content", None)
            parts = getattr(content, "parts", None) or []
            for part in parts:
                part_text = getattr(part, "text", None)
                if part_text:
                    chunks.append(str(part_text).strip())
        return "\n".join(chunk for chunk in chunks if chunk).strip()

