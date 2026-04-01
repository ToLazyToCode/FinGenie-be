import asyncio
import glob
import hashlib
import json
import logging
import os
import re
import time
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
from uuid import uuid4

try:
    import joblib
except ModuleNotFoundError:  # pragma: no cover - runtime environment dependent
    joblib = None

try:
    import numpy as np
except ModuleNotFoundError:  # pragma: no cover - runtime environment dependent
    np = None

try:
    from google import genai
except ModuleNotFoundError:  # pragma: no cover - runtime environment dependent
    genai = None

from .config import Settings, get_settings
from .feature_client import FeatureClient

logger = logging.getLogger(__name__)


class InferenceEngine:
    """Model inference engine with deterministic fallback and optional Gemini helpers."""

    FEATURE_COLUMNS = [
        "rolling_7d_spend",
        "volatility_score",
        "tracking_frequency",
        "avg_transaction_amount",
        "transaction_count_7d",
        "weekday_vs_weekend_ratio",
        "emotional_spending_score",
    ]

    def __init__(
        self,
        settings: Optional[Settings] = None,
        feature_client: Optional[FeatureClient] = None,
    ) -> None:
        self.settings = settings or get_settings()
        self.feature_client = feature_client

        self.model: Any = None
        self.model_name = "spending_predictor"
        self.model_version = self.settings.ai_model_version
        self.loaded_at: Optional[datetime] = None
        self.prediction_count = 0

        # Kept for compatibility with existing LLM utility methods.
        self.last_llm_failure_at = 0.0
        self.client: Any = None
        if self.settings.gemini_api_key and genai is not None:
            try:
                self.client = genai.Client(api_key=self.settings.gemini_api_key)
            except Exception as exc:
                logger.warning("Gemini client initialization failed: %s", exc)

    async def load_model(self) -> None:
        """Load model from disk if available. Falls back to rule-based mode otherwise."""
        if joblib is None:
            self.model = None
            self.loaded_at = None
            logger.warning("joblib is unavailable; using fallback predictions")
            return

        model_path = await asyncio.to_thread(self._resolve_model_path)
        if not model_path:
            self.model = None
            self.loaded_at = None
            logger.warning(
                "No model file found under %s; using fallback predictions",
                self.settings.ai_model_path,
            )
            return

        try:
            self.model = await asyncio.to_thread(joblib.load, model_path)
            self.model_version = self._extract_model_version(model_path) or self.settings.ai_model_version
            self.loaded_at = datetime.now(timezone.utc)
            logger.info("Loaded model from %s (version=%s)", model_path, self.model_version)
        except Exception as exc:
            self.model = None
            self.loaded_at = None
            logger.exception("Failed to load model from %s: %s", model_path, exc)
            if not self.settings.ai_fallback_enabled:
                raise

    def is_model_loaded(self) -> bool:
        return self.model is not None

    def get_model_info(self) -> Dict[str, Any]:
        return {
            "model_name": self.model_name,
            "model_version": self.model_version if self.model else f"{self.model_name}-fallback",
            "feature_version": self.settings.feature_version,
            "loaded_at": self.loaded_at.isoformat() if self.loaded_at else "not_loaded",
            "prediction_count": self.prediction_count,
        }

    async def predict(
        self,
        user_id: int,
        correlation_id: Optional[str] = None,
        prediction_id: Optional[str] = None,
        transaction_id: Optional[str] = None,
    ) -> Dict[str, Any]:
        start_ts = time.perf_counter()
        features = await self._get_user_features(user_id)
        feature_hash = str(features.get("feature_hash") or self._build_feature_hash(features))

        fallback_used = False
        fallback_reason: Optional[str] = None

        if self.model is not None:
            try:
                feature_vector = self._build_feature_vector(features)
                timeout_seconds = max(self.settings.ai_inference_timeout_ms, 1) / 1000.0
                raw_score = await asyncio.wait_for(
                    asyncio.to_thread(self._run_model_prediction, feature_vector),
                    timeout=timeout_seconds,
                )
                predicted_amount = max(0.0, float(raw_score))
                confidence = self._estimate_confidence(features, model_failed=False)
            except Exception as exc:
                logger.exception("Model prediction failed for user %s: %s", user_id, exc)
                if not self.settings.ai_fallback_enabled:
                    raise
                predicted_amount = self._rule_based_prediction(features)
                raw_score = predicted_amount
                confidence = self._estimate_confidence(features, model_failed=True)
                fallback_used = True
                fallback_reason = "model_inference_failed"
        else:
            if not self.settings.ai_fallback_enabled:
                raise RuntimeError("Model not loaded and fallback is disabled")
            predicted_amount = self._rule_based_prediction(features)
            raw_score = predicted_amount
            confidence = self._estimate_confidence(features, model_failed=True)
            fallback_used = True
            fallback_reason = "model_not_loaded"

        risk_score = self._estimate_risk(features)
        predicted_category = self._predict_category(features)
        inference_latency_ms = int((time.perf_counter() - start_ts) * 1000)
        self.prediction_count += 1

        return {
            "user_id": user_id,
            "prediction_id": prediction_id or str(uuid4()),
            "transaction_id": transaction_id,
            "predicted_amount": round(float(predicted_amount), 2),
            "predicted_category": predicted_category,
            "confidence": round(float(confidence), 4),
            "risk_score": round(float(risk_score), 4),
            "model_version": self.model_version if self.model else f"{self.model_name}-fallback",
            "feature_hash": feature_hash,
            "raw_score": round(float(raw_score), 6),
            "explanation": {
                "method": "model_inference" if self.model and not fallback_used else "rule_based_fallback",
                "feature_coverage": round(self._feature_coverage(features), 4),
                "volatility_score": round(self._to_float(features.get("volatility_score"), 0.35), 4),
                "emotional_spending_score": round(self._emotional_score(features), 4),
                "correlation_id": correlation_id,
            },
            "inference_latency_ms": inference_latency_ms,
            "fallback_used": fallback_used,
            "fallback_reason": fallback_reason,
        }

    def chat(
        self,
        messages: List[Dict[str, str]],
        model: Optional[str] = None,
    ) -> Dict[str, Any]:
        if self._llm_in_cooldown():
            return {
                "success": False,
                "source": "gemini",
                "error": "LLM temporarily in cooldown after previous failure.",
            }
        if not self.client:
            return {
                "success": False,
                "source": "gemini",
                "error": "Gemini client not configured.",
            }

        try:
            system_parts = []
            user_parts = []

            for msg in messages:
                role = msg.get("role", "user")
                content = msg.get("content", "")
                if not content:
                    continue
                if role == "system":
                    system_parts.append(content)
                else:
                    user_parts.append(f"{role}: {content}")

            prompt = "\n\n".join(user_parts).strip() or "Hello"

            kwargs = {}
            if system_parts:
                kwargs["config"] = {"system_instruction": "\n".join(system_parts)}

            response = self.client.models.generate_content(
                model=model or self.settings.gemini_model,
                contents=prompt,
                **kwargs,
            )

            return {
                "success": True,
                "source": "gemini",
                "model": model or self.settings.gemini_model,
                "content": response.text or "",
            }
        except Exception as exc:
            self.last_llm_failure_at = time.time()
            logger.exception("Gemini chat failed: %s", exc)
            return {
                "success": False,
                "source": "gemini",
                "error": str(exc),
            }

    def generate(
        self,
        prompt: str,
        model: Optional[str] = None,
    ) -> Dict[str, Any]:
        if self._llm_in_cooldown():
            return {
                "success": False,
                "source": "gemini",
                "error": "LLM temporarily in cooldown after previous failure.",
            }
        if not self.client:
            return {
                "success": False,
                "source": "gemini",
                "error": "Gemini client not configured.",
            }

        try:
            response = self.client.models.generate_content(
                model=model or self.settings.gemini_model,
                contents=prompt,
            )
            return {
                "success": True,
                "source": "gemini",
                "model": model or self.settings.gemini_model,
                "content": response.text or "",
            }
        except Exception as exc:
            self.last_llm_failure_at = time.time()
            logger.exception("Gemini generate failed: %s", exc)
            return {
                "success": False,
                "source": "gemini",
                "error": str(exc),
            }

    def _run_model_prediction(self, feature_vector: Any) -> float:
        prediction = self.model.predict(feature_vector)
        if np is not None and isinstance(prediction, np.ndarray):
            return float(prediction[0])
        if isinstance(prediction, (list, tuple)):
            return float(prediction[0])
        return float(prediction)

    async def _get_user_features(self, user_id: int) -> Dict[str, Any]:
        if not self.feature_client:
            return {}
        try:
            features = await self.feature_client.get_features(user_id)
            return features or {}
        except Exception as exc:
            logger.error("Failed to fetch features for user %s: %s", user_id, exc)
            return {}

    def _build_feature_vector(self, features: Dict[str, Any]) -> Any:
        rolling_7d_spend = self._to_float(features.get("rolling_7d_spend"), 0.0)
        volatility_score = self._clip(self._to_float(features.get("volatility_score"), 0.35), 0.0, 1.0)
        tracking_frequency = self._to_float(
            features.get("tracking_frequency"),
            self._to_float(features.get("transaction_count_7d"), 0.0) / 7.0,
        )
        transaction_count_7d = self._to_float(
            features.get("transaction_count_7d"),
            max(tracking_frequency * 7.0, 1.0),
        )
        avg_transaction_amount = self._to_float(
            features.get("avg_transaction_amount"),
            rolling_7d_spend / max(transaction_count_7d, 1.0),
        )
        weekday_vs_weekend_ratio = self._to_float(features.get("weekday_vs_weekend_ratio"), 1.0)
        emotional_spending_score = self._emotional_score(features)

        values = [[
            rolling_7d_spend,
            volatility_score,
            tracking_frequency,
            avg_transaction_amount,
            transaction_count_7d,
            weekday_vs_weekend_ratio,
            emotional_spending_score,
        ]]
        if np is None:
            return values
        return np.array(values, dtype=np.float64)

    def _rule_based_prediction(self, features: Dict[str, Any]) -> float:
        rolling_7d_spend = self._to_float(features.get("rolling_7d_spend"), 0.0)
        transaction_count_7d = self._to_float(features.get("transaction_count_7d"), 0.0)
        avg_transaction_amount = self._to_float(features.get("avg_transaction_amount"), 0.0)

        if rolling_7d_spend > 0:
            base_daily = rolling_7d_spend / 7.0
        elif transaction_count_7d > 0 and avg_transaction_amount > 0:
            base_daily = (transaction_count_7d * avg_transaction_amount) / 7.0
        else:
            base_daily = 500_000.0

        volatility_score = self._clip(self._to_float(features.get("volatility_score"), 0.35), 0.0, 1.0)
        emotional_spending_score = self._emotional_score(features)
        savings_capacity = self._clip(self._to_float(features.get("survey_savings_capacity"), 0.5), 0.0, 1.0)

        adjustment = (
            1.0
            + (0.25 * volatility_score)
            + (0.15 * emotional_spending_score)
            + (0.10 * (1.0 - savings_capacity))
        )
        return max(0.0, base_daily * adjustment)

    def _predict_category(self, features: Dict[str, Any]) -> str:
        buckets = features.get("rolling_7d_spend_by_category") or features.get("category_distribution")
        if isinstance(buckets, str):
            try:
                buckets = json.loads(buckets)
            except (json.JSONDecodeError, TypeError):
                buckets = None

        if isinstance(buckets, dict) and buckets:
            best_key = max(
                buckets,
                key=lambda k: self._to_float(buckets.get(k), 0.0),
            )
            return str(best_key)

        return str(features.get("top_spending_category") or "GENERAL")

    def _estimate_confidence(self, features: Dict[str, Any], model_failed: bool) -> float:
        coverage = self._feature_coverage(features)
        baseline = 0.75 if not model_failed else 0.45
        score = (0.65 * baseline) + (0.35 * coverage)
        return self._clip(score, 0.25, 0.95)

    def _estimate_risk(self, features: Dict[str, Any]) -> float:
        volatility = self._clip(self._to_float(features.get("volatility_score"), 0.35), 0.0, 1.0)
        emotional = self._emotional_score(features)
        debt_risk = self._clip(self._to_float(features.get("survey_debt_risk_score"), 0.5), 0.0, 1.0)
        tracking_frequency = self._to_float(features.get("tracking_frequency"), 0.0)

        stability = self._clip(tracking_frequency / 5.0, 0.0, 1.0)
        risk = (
            0.35 * volatility
            + 0.30 * emotional
            + 0.25 * debt_risk
            + 0.10 * (1.0 - stability)
        )
        return self._clip(risk, 0.0, 1.0)

    def _feature_coverage(self, features: Dict[str, Any]) -> float:
        available = 0
        for name in self.FEATURE_COLUMNS:
            value = features.get(name)
            if value is not None and str(value) != "":
                available += 1
        return available / len(self.FEATURE_COLUMNS)

    def _emotional_score(self, features: Dict[str, Any]) -> float:
        score = self._to_float(features.get("emotional_spending_score"), None)
        if score is None:
            score = self._to_float(features.get("survey_overspending_score"), 0.5)
        return self._clip(score, 0.0, 1.0)

    @staticmethod
    def _to_float(value: Any, default: Optional[float]) -> Optional[float]:
        if value is None:
            return default
        try:
            return float(value)
        except (TypeError, ValueError):
            return default

    @staticmethod
    def _clip(value: Optional[float], minimum: float, maximum: float) -> float:
        if value is None:
            return minimum
        return min(max(value, minimum), maximum)

    def _build_feature_hash(self, features: Dict[str, Any]) -> str:
        serialized = json.dumps(features, sort_keys=True, default=str, separators=(",", ":"))
        return hashlib.sha256(serialized.encode("utf-8")).hexdigest()

    def _resolve_model_path(self) -> Optional[str]:
        configured = os.path.join(
            self.settings.ai_model_path,
            f"{self.model_name}_{self.settings.ai_model_version}.joblib",
        )
        if os.path.exists(configured):
            return configured

        pattern = os.path.join(self.settings.ai_model_path, f"{self.model_name}_*.joblib")
        candidates = glob.glob(pattern)
        if not candidates:
            return None
        return max(candidates, key=os.path.getmtime)

    @staticmethod
    def _extract_model_version(path: str) -> Optional[str]:
        match = re.search(r"spending_predictor_(.+)\.joblib$", os.path.basename(path))
        if not match:
            return None
        return match.group(1)

    def _llm_in_cooldown(self) -> bool:
        if self.last_llm_failure_at <= 0:
            return False
        return (time.time() - self.last_llm_failure_at) < self.settings.llm_failure_cooldown_seconds
