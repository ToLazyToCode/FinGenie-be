"""
FinGenie AI Service - Insight Engine
"""
import logging
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from app.config import Settings
from app.feature_client import FeatureClient
from app.llm_service import GeminiLLMService
from app.persona_prompts import build_insight_system_instruction, infer_language_code_from_text

logger = logging.getLogger(__name__)


class InsightEngine:
    """
    Generates personalized financial insights by combining:
    - behavioral features (FeatureSnapshot / Redis feature store)
    - transaction pattern analysis
    - LLM summarization
    """

    def __init__(self, settings: Settings, feature_client: FeatureClient):
        self.settings = settings
        self.feature_client = feature_client
        self.llm_service = GeminiLLMService(settings)

    async def generate_insight(
        self,
        user_id: int,
        insight_type: str = "FINANCIAL_SUMMARY",
        message: Optional[str] = None,
        feature_snapshot: Optional[Dict[str, Any]] = None,
        recent_transactions: Optional[List[Dict[str, Any]]] = None,
        user_segment: Optional[str] = None,
    ) -> Dict[str, Any]:
        normalized_type = (insight_type or "FINANCIAL_SUMMARY").upper()
        features = feature_snapshot or await self.feature_client.get_features(user_id) or {}
        transactions = recent_transactions or []
        segment = user_segment or str(features.get("behavioral_segment") or "MODERATE_MANAGER")

        if normalized_type == "PET_CHAT":
            return self._generate_pet_chat(message, features, segment)

        pattern = self._analyze_transaction_pattern(transactions, features)
        overspending_alert = self._build_overspending_alert(pattern, features)
        suggestions = self._build_saving_suggestions(segment, pattern, overspending_alert)
        weekly_insight = self._build_weekly_spending_insight(pattern, segment, overspending_alert)
        summary = await self._summarize_with_llm(
            user_id=user_id,
            segment=segment,
            features=features,
            pattern=pattern,
            weekly_insight=weekly_insight,
            suggestions=suggestions,
            overspending_alert=overspending_alert,
            user_message=message,
        )

        return {
            "userId": user_id,
            "insightType": normalized_type,
            "userSegment": segment,
            "weeklySpendingInsight": weekly_insight,
            "overspendingAlert": overspending_alert,
            "savingSuggestions": suggestions,
            "summary": summary,
            "generatedAt": datetime.now(timezone.utc).isoformat(),
        }

    def _analyze_transaction_pattern(
        self,
        recent_transactions: List[Dict[str, Any]],
        features: Dict[str, Any],
    ) -> Dict[str, Any]:
        if recent_transactions:
            expenses = [self._to_float(tx.get("amount")) for tx in recent_transactions]
            weekly_total = round(sum(expenses), 2)
            tx_count = len(expenses)
            avg = round(weekly_total / tx_count, 2) if tx_count else 0.0

            midpoint = tx_count // 2
            first = sum(expenses[:midpoint]) if midpoint > 0 else weekly_total
            second = sum(expenses[midpoint:]) if midpoint > 0 else weekly_total
            trend_ratio = (second - first) / max(first, 1.0)
            trend = "UP" if trend_ratio > 0.10 else "DOWN" if trend_ratio < -0.10 else "STABLE"

            top_category = self._extract_top_category_from_transactions(recent_transactions)
            return {
                "weekly_total_spend": weekly_total,
                "transaction_count": tx_count,
                "average_transaction_amount": avg,
                "trend": trend,
                "trend_ratio": round(trend_ratio, 4),
                "top_category": top_category,
            }

        rolling_7d = self._to_float(features.get("rolling_7d_spend"))
        tx_count = int(self._to_float(features.get("transaction_count_7d")) or 0)
        avg = round(rolling_7d / max(tx_count, 1), 2) if rolling_7d > 0 else 0.0
        volatility = self._to_float(features.get("volatility_score"))
        trend = "UP" if volatility > 0.65 else "STABLE"
        top_category = self._extract_top_category_from_features(features)

        return {
            "weekly_total_spend": round(rolling_7d, 2),
            "transaction_count": tx_count,
            "average_transaction_amount": avg,
            "trend": trend,
            "trend_ratio": round(volatility - 0.5, 4),
            "top_category": top_category,
        }

    def _build_overspending_alert(
        self,
        pattern: Dict[str, Any],
        features: Dict[str, Any],
    ) -> Dict[str, Any]:
        survey_over = self._to_float(features.get("survey_overspending_score"))
        if survey_over <= 0:
            survey_over = 0.5

        volatility = self._to_float(features.get("volatility_score"))
        if volatility <= 0:
            volatility = 0.5

        trend_ratio = abs(self._to_float(pattern.get("trend_ratio")))
        normalized_trend = min(trend_ratio, 1.0)

        risk_score = (
            (survey_over * 0.55)
            + (volatility * 0.25)
            + (normalized_trend * 0.20)
        )
        risk_score = round(min(max(risk_score, 0.0), 1.0), 4)

        if risk_score >= 0.75:
            level = "HIGH"
        elif risk_score >= 0.55:
            level = "MEDIUM"
        else:
            level = "LOW"

        return {
            "isAlert": level != "LOW",
            "level": level,
            "score": risk_score,
            "message": self._alert_message(level, pattern),
        }

    def _build_saving_suggestions(
        self,
        segment: str,
        pattern: Dict[str, Any],
        overspending_alert: Dict[str, Any],
    ) -> List[str]:
        suggestions: List[str] = []

        if overspending_alert["level"] == "HIGH":
            suggestions.append("Set a strict daily cap for the next 7 days")
            suggestions.append("Pause non-essential purchases until weekend review")
        elif overspending_alert["level"] == "MEDIUM":
            suggestions.append("Set category-level limits for your top spending category")
            suggestions.append("Review each purchase above your weekly average")
        else:
            suggestions.append("Auto-transfer a fixed amount to savings after payday")

        top_category = str(pattern.get("top_category") or "GENERAL")
        suggestions.append(f"Reduce {top_category} spending by 10% this week")

        upper_segment = segment.upper()
        if "AT_RISK" in upper_segment:
            suggestions.append("Use cash-only mode for discretionary spending this week")
        elif "DISCIPLINED" in upper_segment or "STRATEGIC" in upper_segment:
            suggestions.append("Increase your weekly savings target by 5%")
        else:
            suggestions.append("Schedule a 15-minute weekly budget check-in")

        return suggestions[:3]

    def _build_weekly_spending_insight(
        self,
        pattern: Dict[str, Any],
        segment: str,
        overspending_alert: Dict[str, Any],
    ) -> str:
        weekly_total = self._to_float(pattern.get("weekly_total_spend"))
        tx_count = int(pattern.get("transaction_count") or 0)
        trend = str(pattern.get("trend") or "STABLE")
        category = str(pattern.get("top_category") or "GENERAL")
        risk_level = overspending_alert.get("level", "LOW")

        return (
            f"This week you recorded {tx_count} transactions totaling {weekly_total:,.0f} VND. "
            f"Your spending trend is {trend} with {category} as the top category. "
            f"Given your segment {segment}, overspending risk is currently {risk_level}."
        )

    async def _summarize_with_llm(
        self,
        user_id: int,
        segment: str,
        features: Dict[str, Any],
        pattern: Dict[str, Any],
        weekly_insight: str,
        suggestions: List[str],
        overspending_alert: Dict[str, Any],
        user_message: Optional[str] = None,
    ) -> str:
        language_code = infer_language_code_from_text(user_message, default="en")
        prompt = (
            "Write a concise personalized financial summary in 2-3 sentences.\n"
            f"User: {user_id}\n"
            f"Segment: {segment}\n"
            f"Behavioral features: {features}\n"
            f"Transaction pattern: {pattern}\n"
            f"Weekly insight: {weekly_insight}\n"
            f"Overspending alert: {overspending_alert}\n"
            f"Saving suggestions: {suggestions}\n"
            "Output only the summary paragraph."
        )

        text = await self.llm_service.try_generate_text(
            prompt,
            system_instruction=build_insight_system_instruction(language_code),
        )
        if text:
            return text
        logger.warning("Insight LLM summarization failed or unavailable, using fallback summary")

        return (
            f"{weekly_insight} Focus this week: {suggestions[0]} "
            f"and monitor your {pattern.get('top_category', 'GENERAL')} expenses daily."
        )

    def _generate_pet_chat(
        self,
        message: Optional[str],
        features: Dict[str, Any],
        segment: str,
    ) -> Dict[str, Any]:
        user_message = (message or "").strip()
        risk = self._to_float(features.get("survey_overspending_score"))
        risk = 0.5 if risk <= 0 else risk

        if risk > 0.7:
            mood = "CONCERNED"
            happiness = 45
            pet_message = (
                "I noticed your spending risk is high today. "
                "Let's keep one purchase small and track it together."
            )
        elif risk > 0.5:
            mood = "NEUTRAL"
            happiness = 60
            pet_message = (
                "You're doing okay. A quick budget check can keep us on track this week."
            )
        else:
            mood = "HAPPY"
            happiness = 78
            pet_message = (
                "Great discipline this week! Let's save a bit more to boost our future goals."
            )

        if user_message:
            pet_message = f"{pet_message} You said: \"{user_message}\""

        return {
            "petMessage": pet_message,
            "mood": mood,
            "happiness": happiness,
            "confidence": 0.82,
            "personality": f"Supportive {segment.title().replace('_', ' ')}",
            "generatedAt": datetime.now(timezone.utc).isoformat(),
        }

    @staticmethod
    def _extract_top_category_from_transactions(transactions: List[Dict[str, Any]]) -> str:
        totals: Dict[str, float] = {}
        for tx in transactions:
            category = str(
                tx.get("categoryName")
                or tx.get("category")
                or tx.get("category_name")
                or "GENERAL"
            ).upper()
            amount = abs(float(tx.get("amount") or 0))
            totals[category] = totals.get(category, 0.0) + amount

        if not totals:
            return "GENERAL"
        return max(totals.items(), key=lambda pair: pair[1])[0]

    @staticmethod
    def _extract_top_category_from_features(features: Dict[str, Any]) -> str:
        dist = features.get("category_distribution")
        if isinstance(dist, dict) and dist:
            try:
                return str(max(dist.items(), key=lambda pair: float(pair[1]))[0]).upper()
            except Exception:
                return "GENERAL"
        return "GENERAL"

    @staticmethod
    def _alert_message(level: str, pattern: Dict[str, Any]) -> str:
        category = str(pattern.get("top_category") or "GENERAL")
        if level == "HIGH":
            return f"High overspending risk detected, especially in {category}."
        if level == "MEDIUM":
            return f"Moderate overspending risk. Watch {category} spending closely."
        return "Overspending risk is currently low."

    @staticmethod
    def _to_float(value: Any) -> float:
        if value is None:
            return 0.0
        if isinstance(value, (int, float)):
            return float(value)
        try:
            return float(value)
        except Exception:
            return 0.0
