"""
FinGenie AI Service - Monthly Saving Plan Advisor
"""
import logging
from typing import Any, Dict, List, Tuple

from app.config import Settings
from app.llm_service import GeminiLLMService
from app.persona_prompts import build_saving_plan_polish_system_instruction

logger = logging.getLogger(__name__)


class SavingPlanAdvisor:
    """
    Generates explanation-only advice for a deterministic saving plan.
    This module never changes numeric allocations.
    """

    def __init__(self, settings: Settings):
        self.settings = settings
        self.llm_service = GeminiLLMService(settings)

    async def generate_advice(
        self,
        saving_capacity: float,
        targets: List[Dict[str, Any]],
        allocations: List[Dict[str, Any]],
        recommendations: List[Dict[str, Any]],
        category_spend: List[Dict[str, Any]],
        language_code: str | None = None,
    ) -> Dict[str, Any]:
        resolved_language = self._normalize_language_code(language_code)
        total_required = sum(self._to_float(item.get("required_monthly")) for item in allocations)
        total_allocated = sum(self._to_float(item.get("allocated_monthly")) for item in allocations)
        gap = max(0.0, total_required - total_allocated)
        feasibility = self._safe_percent(total_allocated, total_required)

        short_summary = self._build_summary(
            saving_capacity=saving_capacity,
            total_required=total_required,
            total_allocated=total_allocated,
            gap=gap,
            feasibility=feasibility,
            language_code=resolved_language,
        )
        actionable_suggestions = self._build_suggestions(recommendations, category_spend, resolved_language)
        risk_warnings = self._build_warnings(saving_capacity, gap, allocations, category_spend, resolved_language)
        friendly_tone = self._build_friendly_tone(gap, feasibility, actionable_suggestions, resolved_language)

        polished_summary, polished_tone = await self._optional_polish(
            short_summary,
            friendly_tone,
            actionable_suggestions,
            risk_warnings,
            resolved_language,
        )
        if polished_summary:
            short_summary = polished_summary
        if polished_tone:
            friendly_tone = polished_tone

        return {
            "short_summary": short_summary,
            "actionable_suggestions": actionable_suggestions[:3],
            "risk_warnings": risk_warnings,
            "friendly_tone": friendly_tone,
        }

    def _build_summary(
        self,
        saving_capacity: float,
        total_required: float,
        total_allocated: float,
        gap: float,
        feasibility: float,
        language_code: str,
    ) -> str:
        is_vietnamese = language_code == "vi"
        if total_required <= 0:
            if is_vietnamese:
                return (
                    "Ke hoach thang hien tai chua co muc tieu can dong. "
                    "Hay dat mot muc tieu tiet kiem ro rang de toi ho tro theo doi."
                )
            return (
                "Your current monthly plan has no required targets yet. "
                "Set one clear savings goal and I will help you track it."
            )

        if gap > 0:
            if is_vietnamese:
                return (
                    f"Ban hien tai co the dap ung khoang {feasibility:.1f}% muc tieu tiet kiem thang. "
                    f"Ban dang thieu {gap:,.2f} trong thang nay, nen can uu tien cac dieu chinh tac dong lon truoc."
                )
            return (
                f"You can currently cover about {feasibility:.1f}% of your monthly saving targets. "
                f"You're short by {gap:,.2f} this month, so we should prioritize the highest-impact adjustments first."
            )

        if is_vietnamese:
            return (
                "Tien do rat tot. Ke hoach hien tai co the dap ung 100% muc tieu thang "
                f"voi nang luc tiet kiem khoang {saving_capacity:,.2f}."
            )
        return (
            f"Great progress. Your current plan can fund 100% of monthly targets "
            f"with available saving capacity around {saving_capacity:,.2f}."
        )

    def _build_suggestions(
        self,
        recommendations: List[Dict[str, Any]],
        category_spend: List[Dict[str, Any]],
        language_code: str,
    ) -> List[str]:
        is_vietnamese = language_code == "vi"
        suggestions: List[str] = []

        for item in recommendations:
            message = (item.get("message") or "").strip()
            if message and message not in suggestions:
                suggestions.append(message)
            if len(suggestions) >= 3:
                break

        for category in category_spend[:3]:
            if len(suggestions) >= 3:
                break
            name = str(category.get("category_name") or "Uncategorized")
            monthly = self._to_float(category.get("monthly_expense"))
            reduce_10 = monthly * 0.10
            if is_vietnamese:
                hint = f"Giam chi tieu {name} 10% de tiet kiem them khoang {reduce_10:,.2f} moi thang."
            else:
                hint = f"Trim {name} spending by 10% to free roughly {reduce_10:,.2f} per month."
            if hint not in suggestions:
                suggestions.append(hint)

        defaults = [
            "Set a small weekly transfer to savings right after income arrives.",
            "Review non-essential spending every weekend and cut one recurring cost.",
            "Track one high-spend category daily to avoid budget drift.",
        ]
        if is_vietnamese:
            defaults = [
                "Dat chuyen khoan tiet kiem nho hang tuan ngay sau khi nhan thu nhap.",
                "Xem lai chi tieu khong thiet yeu moi cuoi tuan va cat mot khoan lap lai.",
                "Theo doi mot danh muc chi lon moi ngay de tranh lech ngan sach.",
            ]

        while len(suggestions) < 3:
            suggestions.append(defaults[len(suggestions)])

        return suggestions[:3]

    def _build_warnings(
        self,
        saving_capacity: float,
        gap: float,
        allocations: List[Dict[str, Any]],
        category_spend: List[Dict[str, Any]],
        language_code: str,
    ) -> List[str]:
        is_vietnamese = language_code == "vi"
        warnings: List[str] = []

        if saving_capacity <= 0:
            warnings.append(
                "Nang luc tiet kiem hien tai bang 0 hoac am; muc tieu tiet kiem co the dung lai neu khong giam chi."
                if is_vietnamese
                else "Your current saving capacity is zero or negative; savings targets may stall without expense cuts."
            )

        if gap > 0:
            warnings.append(
                f"Phat hien khoang thieu muc tieu thang: {gap:,.2f}."
                if is_vietnamese
                else f"Monthly target gap detected: {gap:,.2f}."
            )

        low_feasibility = [
            a for a in allocations
            if self._to_float(a.get("feasibility_score")) < 50.0
        ]
        if low_feasibility:
            warnings.append(
                "Mot so muc tieu duoi 50% kha thi trong thang nay va co the can gian tien do."
                if is_vietnamese
                else "Some targets are under 50% feasible this month and may need deadline stretching."
            )

        if category_spend:
            top = category_spend[0]
            top_name = str(top.get("category_name") or "Uncategorized")
            top_value = self._to_float(top.get("monthly_expense"))
            total_expense = sum(self._to_float(x.get("monthly_expense")) for x in category_spend)
            if total_expense > 0 and (top_value / total_expense) >= 0.45:
                warnings.append(
                    f"Chi tieu dang tap trung o {top_name}; muc do tap trung cao lam tang rui ro vuot ngan sach."
                    if is_vietnamese
                    else f"Spending is concentrated in {top_name}; category concentration increases overspending risk."
                )

        return warnings

    def _build_friendly_tone(self, gap: float, feasibility: float, suggestions: List[str], language_code: str) -> str:
        is_vietnamese = language_code == "vi"
        first_tip = suggestions[0] if suggestions else "small daily spending checks"
        if gap > 0:
            if is_vietnamese:
                return (
                    f"Thu cung FinGenie dang co vu ban. Chung ta da rat gan muc tieu, va buoc nay se giup ro: {first_tip}"
                )
            return (
                f"Your FinGenie pet is cheering for you. We are close, and one smart move now helps a lot: {first_tip}"
            )
        if feasibility >= 100.0:
            if is_vietnamese:
                return "Thu cung FinGenie rat vui hom nay. Hay giu nhip nay de tang chuoi tiet kiem cua ban."
            return (
                "Your FinGenie pet is super happy today. Keep this rhythm and we can level up your savings streak."
            )
        if is_vietnamese:
            return f"Thu cung FinGenie ghi nhan no luc on dinh cua ban. Hay tiep tuc voi buoc nay: {first_tip}"
        return (
            f"Your FinGenie pet says you're doing steady work. Let's keep momentum with this step: {first_tip}"
        )

    async def _optional_polish(
        self,
        summary: str,
        tone: str,
        suggestions: List[str],
        warnings: List[str],
        language_code: str,
    ) -> Tuple[str, str]:
        language_name = "Vietnamese" if language_code == "vi" else "English"
        prompt = (
            "Rewrite the following to be concise and friendly. "
            "Keep the same meaning, no new numbers, no new promises.\n"
            f"Reply strictly in {language_name}.\n"
            f"Summary: {summary}\n"
            f"Tone: {tone}\n"
            f"Suggestions: {suggestions}\n"
            f"Warnings: {warnings}\n"
            "Output exactly two lines:\n"
            "SUMMARY: <text>\n"
            "TONE: <text>"
        )

        text = await self.llm_service.try_generate_text(
            prompt,
            system_instruction=build_saving_plan_polish_system_instruction(language_code),
        )
        if text:
            return self._parse_polish(text)
        logger.info("Saving plan advice LLM polish skipped; using deterministic text")
        return "", ""

    def _parse_polish(self, text: str) -> Tuple[str, str]:
        summary = ""
        tone = ""
        for line in text.splitlines():
            normalized = line.strip()
            if normalized.upper().startswith("SUMMARY:"):
                summary = normalized.split(":", 1)[1].strip()
            elif normalized.upper().startswith("TONE:"):
                tone = normalized.split(":", 1)[1].strip()
        return summary, tone

    def _safe_percent(self, value: float, total: float) -> float:
        if total <= 0:
            return 100.0
        return min(100.0, max(0.0, (value / total) * 100.0))

    def _to_float(self, value: Any) -> float:
        if value is None:
            return 0.0
        if isinstance(value, (int, float)):
            return float(value)
        try:
            return float(value)
        except Exception:
            return 0.0

    @staticmethod
    def _normalize_language_code(value: str | None) -> str:
        normalized = (value or "").strip().lower()
        if normalized.startswith("vi") or "vietnam" in normalized:
            return "vi"
        return "en"
