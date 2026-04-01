"""
FinGenie AI Service - Kafka Consumer
"""
import asyncio
import json
import logging
from typing import Optional, Dict, Any

from aiokafka import AIOKafkaConsumer
from aiokafka.errors import KafkaConnectionError

from app.config import Settings
from app.feature_client import FeatureClient
from app.inference_engine import InferenceEngine
from app.kafka_producer import KafkaProducerService

logger = logging.getLogger(__name__)


class KafkaConsumerService:
    """
    Kafka consumer for processing transaction and user events.
    
    Consumes:
    - transaction-events: Triggers feature updates and predictions
    - user-events: Updates user context
    - survey-events: Updates emotional spending score
    """
    
    def __init__(
        self,
        settings: Settings,
        inference_engine: InferenceEngine,
        kafka_producer: KafkaProducerService,
        feature_client: Optional[FeatureClient] = None,
    ):
        self.settings = settings
        self.inference_engine = inference_engine
        self.kafka_producer = kafka_producer
        self.feature_client = feature_client
        self.consumer: Optional[AIOKafkaConsumer] = None
        self.running = False
    
    async def start(self):
        """Start consuming messages."""
        self.running = True
        
        while self.running:
            try:
                self.consumer = AIOKafkaConsumer(
                    self.settings.kafka_transaction_topic,
                    self.settings.kafka_user_topic,
                    self.settings.kafka_survey_topic,
                    bootstrap_servers=self.settings.kafka_bootstrap_servers,
                    group_id=self.settings.kafka_consumer_group,
                    auto_offset_reset="earliest",
                    enable_auto_commit=False,
                    value_deserializer=self._safe_deserialize,
                )
                
                await self.consumer.start()
                logger.info(f"Kafka consumer started, subscribed to topics")
                
                async for message in self.consumer:
                    if not self.running:
                        break
                    
                    try:
                        await self._process_message(message)
                        await self.consumer.commit()
                    except Exception as e:
                        logger.error(f"Error processing message: {e}")
                        # In production, send to DLQ
                        
            except KafkaConnectionError as e:
                logger.error(f"Kafka connection error: {e}")
                if self.running:
                    logger.info("Retrying in 5 seconds...")
                    await asyncio.sleep(5)
            except Exception as e:
                logger.error(f"Unexpected error in Kafka consumer: {e}")
                if self.running:
                    await asyncio.sleep(5)
            finally:
                if self.consumer:
                    await self.consumer.stop()
    
    async def stop(self):
        """Stop consuming messages."""
        self.running = False
        if self.consumer:
            await self.consumer.stop()
        logger.info("Kafka consumer stopped")
    
    async def _process_message(self, message):
        """Process a single Kafka message."""
        topic = message.topic
        value = message.value

        if isinstance(value, dict) and value.get("_malformed"):
            logger.warning(
                "Skipping malformed Kafka message on topic %s partition %s offset %s",
                topic,
                message.partition,
                message.offset,
            )
            return
        
        logger.debug(f"Processing message from {topic}: {value.get('event_type')}")
        
        if topic == self.settings.kafka_transaction_topic:
            await self._handle_transaction_event(value)
        elif topic == self.settings.kafka_user_topic:
            await self._handle_user_event(value)
        elif topic == self.settings.kafka_survey_topic:
            await self._handle_survey_event(value)
    
    async def _handle_transaction_event(self, event: dict):
        """
        Handle transaction event.
        
        When a transaction is created:
        1. Update feature store (via Spring Boot)
        2. Generate new prediction
        3. Publish prediction result
        """
        event_type = event.get("event_type")
        payload = event.get("payload", {})
        user_id = payload.get("user_id")
        transaction_id = payload.get("transaction_id") or payload.get("transactionId")
        prediction_id = (
            payload.get("prediction_id")
            or payload.get("predictionId")
            or event.get("prediction_id")
            or event.get("predictionId")
        )
        
        if not user_id:
            logger.warning(f"Transaction event missing user_id: {event}")
            return
        
        if event_type == "TRANSACTION_CREATED":
            logger.info(f"Processing transaction for user {user_id}")
            
            # Generate prediction
            try:
                prediction = await self.inference_engine.predict(
                    user_id=user_id,
                    correlation_id=event.get("correlation_id"),
                    prediction_id=prediction_id,
                    transaction_id=transaction_id
                )
                
                # Publish prediction result
                await self.kafka_producer.publish_prediction(
                    prediction,
                    event.get("correlation_id")
                )
                
                logger.info(f"Published prediction for user {user_id}: amount={prediction['predicted_amount']}")
                
            except Exception as e:
                logger.error(f"Failed to generate prediction for user {user_id}: {e}")
    
    async def _handle_user_event(self, event: dict):
        """Handle user event (login, logout, profile update)."""
        event_type = event.get("event_type")
        user_id = event.get("user_id")
        
        if event_type == "USER_LOGIN":
            logger.debug(f"User {user_id} logged in")
            # Could trigger feature pre-warming here
        
        elif event_type == "PROFILE_UPDATED":
            logger.debug(f"User {user_id} profile updated")
            # Could trigger feature refresh
    
    async def _handle_survey_event(self, event: dict):
        """
        Handle survey/feedback event.
        
        Handles:
        - SURVEY_COMPLETED: Sync baseline features from survey responses
        - PROFILE_UPDATED: Refresh user feature cache
        - EMOTIONAL_CHECK_IN: Update emotional spending score
        - FEEDBACK_SUBMITTED: Log for retraining
        """
        event_type = event.get("eventType") or event.get("event_type")
        user_id = event.get("userId") or event.get("user_id")
        
        if not user_id:
            logger.warning(f"Survey event missing user_id: {event}")
            return
        
        if event_type == "SURVEY_COMPLETED":
            survey_response_id = event.get("surveyResponseId")
            logger.info(f"User {user_id} completed behavioral survey (response_id={survey_response_id})")
            
            # Prefer profile snapshot from event; fallback to Spring Boot API.
            await self._sync_survey_baseline_features(user_id, event)
        
        elif event_type == "PROFILE_UPDATED":
            segment = event.get("segment")
            logger.info(f"User {user_id} profile updated: segment={segment}")
            # Refresh feature cache
            await self._refresh_user_features(user_id, segment)
        
        elif event_type == "EMOTIONAL_CHECK_IN":
            emotional_score = event.get("emotional_score")
            logger.info(f"User {user_id} emotional check-in: score={emotional_score}")
            # Feature store update would be handled by Spring Boot
        
        elif event_type == "FEEDBACK_SUBMITTED":
            feedback_type = event.get("feedback_type")
            logger.info(f"User {user_id} submitted feedback: type={feedback_type}")
            # Could trigger model retraining signal
    
    async def _sync_survey_baseline_features(self, user_id: int, event: Optional[dict] = None):
        """
        Sync baseline features from completed behavioral survey.
        
        This is critical for cold-start users who have transaction data yet.
        The survey provides initial feature values for:
        - emotional_spending_score
        - overspending_risk
        - debt_risk
        - savings_capacity
        - behavioral_segment
        """
        if not self.feature_client:
            logger.warning("Feature client not available, cannot sync features for user %s", user_id)
            return

        event_features = self._build_features_from_event(event)
        if event_features:
            await self.feature_client.set_survey_features(user_id, event_features)
            logger.info(
                "Synced survey baseline features from event for user %s: segment=%s",
                user_id,
                event_features.get("behavioral_segment"),
            )
            return

        try:
            import httpx

            url = f"{self.settings.backend_url}/api/v1/behavior/profile"
            async with httpx.AsyncClient(timeout=10.0) as client:
                response = await client.get(url, headers={"X-User-Id": str(user_id)})

            if response.status_code != 200:
                logger.warning(
                    "Failed to fetch behavior profile for user %s from %s: HTTP %s",
                    user_id,
                    url,
                    response.status_code,
                )
                return

            profile = response.json()
            features = {
                "survey_overspending_score": float(profile.get("overspendingScore", 50)) / 100,
                "survey_debt_risk_score": float(profile.get("debtRiskScore", 50)) / 100,
                "survey_savings_capacity": float(profile.get("savingsCapacityScore", 50)) / 100,
                "survey_anxiety_index": float(profile.get("financialAnxietyIndex", 50)) / 100,
                "behavioral_segment": profile.get("segment", "MODERATE_MANAGER"),
                "segment_confidence": float(profile.get("segmentConfidence", 0.7)),
                "survey_completed": True,
                "survey_completed_at": profile.get("surveyCompletedAt"),
                "risk_level": profile.get("riskLevel", "MEDIUM"),
            }
            await self.feature_client.set_survey_features(user_id, features)
            logger.info(
                "Synced survey baseline features from backend for user %s: segment=%s",
                user_id,
                features.get("behavioral_segment"),
            )
        except Exception as e:
            logger.error(
                "Error syncing survey baseline features for user %s via backend URL %s: %s",
                user_id,
                self.settings.backend_url,
                e,
            )
    
    async def _refresh_user_features(self, user_id: int, segment: str = None):
        """Refresh user features after profile update."""
        try:
            if self.feature_client and segment:
                await self.feature_client.set_features(
                    user_id,
                    {"behavioral_segment": segment},
                    ttl_seconds=self.settings.feature_cache_ttl_seconds
                )
                logger.debug(f"Updated segment for user {user_id}: {segment}")
        except Exception as e:
            logger.error(f"Error refreshing features for user {user_id}: {e}")

    @staticmethod
    def _build_features_from_event(event: Optional[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        if not event:
            return None

        required_keys = (
            "overspendingScore",
            "debtRiskScore",
            "savingsCapacityScore",
            "financialAnxietyIndex",
            "segment",
        )
        if not all(key in event and event.get(key) is not None for key in required_keys):
            return None

        return {
            "survey_overspending_score": float(event.get("overspendingScore", 50)) / 100,
            "survey_debt_risk_score": float(event.get("debtRiskScore", 50)) / 100,
            "survey_savings_capacity": float(event.get("savingsCapacityScore", 50)) / 100,
            "survey_anxiety_index": float(event.get("financialAnxietyIndex", 50)) / 100,
            "behavioral_segment": str(event.get("segment", "MODERATE_MANAGER")),
            "segment_confidence": float(event.get("segmentConfidence", 0.7)),
            "survey_completed": True,
            "survey_completed_at": event.get("surveyCompletedAt"),
            "risk_level": str(event.get("riskLevel", "MEDIUM")),
        }

    @staticmethod
    def _safe_deserialize(message: bytes) -> Dict[str, Any]:
        try:
            return json.loads(message.decode("utf-8"))
        except Exception:
            return {"_malformed": True}
