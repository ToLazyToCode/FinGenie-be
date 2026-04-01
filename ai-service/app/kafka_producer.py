"""
FinGenie AI Service - Kafka Producer
"""
import json
import logging
from datetime import datetime
from typing import Optional, Dict, Any
from uuid import uuid4

from aiokafka import AIOKafkaProducer
from aiokafka.errors import KafkaConnectionError

from app.config import Settings

logger = logging.getLogger(__name__)


class KafkaProducerService:
    """
    Kafka producer for publishing prediction results.
    
    Publishes to:
    - model-predictions: Prediction results for Spring Boot consumption
    """
    
    def __init__(self, settings: Settings):
        self.settings = settings
        self.producer: Optional[AIOKafkaProducer] = None
        self._connected = False
    
    async def start(self):
        """Start the Kafka producer."""
        try:
            self.producer = AIOKafkaProducer(
                bootstrap_servers=self.settings.kafka_bootstrap_servers,
                value_serializer=lambda v: json.dumps(v, default=str).encode("utf-8"),
                key_serializer=lambda k: k.encode("utf-8") if k else None,
                acks="all",
                retry_backoff_ms=100
            )
            await self.producer.start()
            self._connected = True
            logger.info("Kafka producer started")
        except KafkaConnectionError as e:
            logger.error(f"Failed to start Kafka producer: {e}")
            self._connected = False
    
    async def stop(self):
        """Stop the Kafka producer."""
        if self.producer:
            await self.producer.stop()
            self._connected = False
            logger.info("Kafka producer stopped")
    
    def is_connected(self) -> bool:
        """Check if producer is connected."""
        return self._connected
    
    async def publish_prediction(
        self,
        prediction: Dict[str, Any],
        correlation_id: Optional[str] = None
    ):
        """
        Publish a prediction result to Kafka.
        
        Message format follows ModelPredictionEvent schema.
        """
        if not self.producer:
            logger.error("Kafka producer not started")
            return
        
        user_id = prediction.get("user_id")
        
        message = {
            "schema_version": "v1",
            "event_id": str(uuid4()),
            "event_type": "MODEL_PREDICTION",
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "correlation_id": correlation_id or str(uuid4()),
            "source_service": self.settings.service_name,
            
            "user_id": user_id,
            "prediction_id": prediction.get("prediction_id"),
            "transaction_id": prediction.get("transaction_id"),
            "prediction_type": "SPENDING_GUESS",
            "request_timestamp": datetime.utcnow().isoformat() + "Z",
            
            "model_version": prediction.get("model_version"),
            "model_name": "spending_predictor",
            "feature_hash": prediction.get("feature_hash"),
            "feature_version": self.settings.feature_version,
            
            "predicted_amount": prediction.get("predicted_amount"),
            "predicted_category": prediction.get("predicted_category"),
            "confidence": prediction.get("confidence"),
            "raw_score": prediction.get("raw_score"),
            "risk_score": prediction.get("risk_score"),
            
            "explanation": prediction.get("explanation"),
            
            "inference_latency_ms": prediction.get("inference_latency_ms"),
            "fallback_used": prediction.get("fallback_used", False),
            "fallback_reason": prediction.get("fallback_reason"),
        }
        
        try:
            key = str(user_id) if user_id else None
            await self.producer.send_and_wait(
                self.settings.kafka_prediction_topic,
                value=message,
                key=key
            )
            logger.debug(f"Published prediction for user {user_id}")
        except Exception as e:
            logger.error(f"Failed to publish prediction: {e}")
            raise
    
    async def publish_feature_update(
        self,
        user_id: int,
        feature_hash: str,
        trigger_event: str
    ):
        """Publish a feature update notification."""
        if not self.producer:
            return
        
        message = {
            "schema_version": "v1",
            "event_id": str(uuid4()),
            "event_type": "FEATURE_UPDATE",
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "source_service": self.settings.service_name,
            
            "user_id": user_id,
            "feature_hash": feature_hash,
            "feature_version": self.settings.feature_version,
            "update_type": "INCREMENTAL",
            "trigger_event": trigger_event,
        }
        
        try:
            await self.producer.send_and_wait(
                self.settings.kafka_feature_topic,
                value=message,
                key=str(user_id)
            )
        except Exception as e:
            logger.error(f"Failed to publish feature update: {e}")
