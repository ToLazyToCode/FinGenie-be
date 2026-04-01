"""
FinGenie AI Service - Feature Client (Redis)
"""
import json
import logging
from typing import Optional, Dict, Any

import redis.asyncio as redis

from app.config import Settings

logger = logging.getLogger(__name__)


class FeatureClient:
    """
    Feature client for fetching user features from Redis.
    
    Features cached in Redis:
    - rolling_7d_spend: Total spending in last 7 days
    - rolling_7d_spend_by_category: Spend breakdown by category
    - category_distribution: Category percentages
    - volatility_score: Spending volatility (0-1)
    - tracking_frequency: Transactions per day
    - emotional_spending_score: From survey responses
    - feature_hash: Hash for reproducibility
    """
    
    FEATURE_PREFIX = "user:features:"
    
    def __init__(self, settings: Settings):
        self.settings = settings
        self.redis_client: Optional[redis.Redis] = None
    
    async def connect(self) -> bool:
        """Connect to Redis."""
        try:
            self.redis_client = redis.Redis(
                host=self.settings.redis_host,
                port=self.settings.redis_port,
                db=self.settings.redis_db,
                password=self.settings.redis_password or None,
                decode_responses=True
            )
            await self.redis_client.ping()
            logger.info(f"Connected to Redis at {self.settings.redis_host}:{self.settings.redis_port}")
            return True
        except Exception as e:
            logger.error(f"Failed to connect to Redis: {e}")
            return False
    
    async def disconnect(self):
        """Disconnect from Redis."""
        if self.redis_client:
            await self.redis_client.close()
            logger.info("Disconnected from Redis")
    
    async def ping(self) -> bool:
        """Check if Redis is connected."""
        if not self.redis_client:
            return False
        try:
            await self.redis_client.ping()
            return True
        except:
            return False
    
    async def get_features(self, user_id: int) -> Optional[Dict[str, Any]]:
        """
        Get features for a user from Redis.
        
        Returns None if not found or error.
        """
        if not self.redis_client:
            logger.error("Redis client not connected")
            return None
        
        try:
            key = f"{self.FEATURE_PREFIX}{user_id}"
            data = await self.redis_client.hgetall(key)
            
            if not data:
                logger.debug(f"No features found for user {user_id}")
                return None
            
            # Parse stored values
            features = {}
            for k, v in data.items():
                features[k] = self._parse_feature_value(v)
            
            logger.debug(f"Retrieved features for user {user_id}: {len(features)} fields")
            return features
            
        except Exception as e:
            logger.error(f"Error fetching features for user {user_id}: {e}")
            return None
    
    async def get_feature(self, user_id: int, feature_name: str) -> Optional[Any]:
        """Get a single feature value."""
        if not self.redis_client:
            return None
        
        try:
            key = f"{self.FEATURE_PREFIX}{user_id}"
            value = await self.redis_client.hget(key, feature_name)
            
            if value is None:
                return None
            
            return self._parse_feature_value(value)
                    
        except Exception as e:
            logger.error(f"Error fetching feature {feature_name} for user {user_id}: {e}")
            return None
    
    async def set_features(
        self,
        user_id: int,
        features: Dict[str, Any],
        ttl_seconds: Optional[int] = None
    ) -> bool:
        """
        Set features for a user in Redis.
        
        Used primarily for testing and manual updates.
        """
        if not self.redis_client:
            logger.error("Redis client not connected")
            return False
        
        try:
            key = f"{self.FEATURE_PREFIX}{user_id}"
            
            # Serialize values
            serialized = {}
            for k, v in features.items():
                serialized[k] = json.dumps(v)
            
            await self.redis_client.hset(key, mapping=serialized)
            
            if ttl_seconds:
                await self.redis_client.expire(key, ttl_seconds)
            
            logger.debug(f"Set features for user {user_id}: {len(features)} fields")
            return True
            
        except Exception as e:
            logger.error(f"Error setting features for user {user_id}: {e}")
            return False
    
    async def delete_features(self, user_id: int) -> bool:
        """Delete features for a user (for GDPR compliance)."""
        if not self.redis_client:
            return False
        
        try:
            key = f"{self.FEATURE_PREFIX}{user_id}"
            await self.redis_client.delete(key)
            logger.info(f"Deleted features for user {user_id}")
            return True
        except Exception as e:
            logger.error(f"Error deleting features for user {user_id}: {e}")
            return False
    
    async def get_feature_hash(self, user_id: int) -> Optional[str]:
        """Get the feature hash for reproducibility checks."""
        return await self.get_feature(user_id, "feature_hash")
    
    async def set_survey_features(
        self,
        user_id: int,
        survey_features: Dict[str, Any]
    ) -> bool:
        """
        Set survey-based baseline features for a user.
        
        Survey features provide cold-start values for users without transaction history.
        These features are prefixed with 'survey_' and include:
        - survey_overspending_score: Risk of overspending (0-1)
        - survey_debt_risk_score: Risk of debt (0-1)
        - survey_savings_capacity: Ability to save (0-1)
        - survey_anxiety_index: Financial anxiety level (0-1)
        - behavioral_segment: Classification (enum string)
        - segment_confidence: Confidence in segment classification (0-1)
        """
        if not self.redis_client:
            logger.error("Redis client not connected")
            return False
        
        try:
            key = f"{self.FEATURE_PREFIX}{user_id}"
            
            # Serialize values
            serialized = {}
            for k, v in survey_features.items():
                if isinstance(v, (dict, list)):
                    serialized[k] = json.dumps(v)
                elif isinstance(v, bool):
                    serialized[k] = str(v).lower()
                else:
                    serialized[k] = str(v)
            
            await self.redis_client.hset(key, mapping=serialized)
            
            # Survey features don't expire (they're baseline)
            # but we set a long TTL as safety
            await self.redis_client.expire(key, 86400 * 180)  # 180 days
            
            logger.info(f"Set survey baseline features for user {user_id}: {len(survey_features)} fields")
            return True
            
        except Exception as e:
            logger.error(f"Error setting survey features for user {user_id}: {e}")
            return False
    
    async def get_survey_features(self, user_id: int) -> Optional[Dict[str, Any]]:
        """
        Get survey-based features for a user.
        
        Returns features prefixed with 'survey_' and behavioral segment info.
        """
        if not self.redis_client:
            return None
        
        try:
            key = f"{self.FEATURE_PREFIX}{user_id}"
            data = await self.redis_client.hgetall(key)
            
            if not data:
                return None
            
            # Filter to survey features only
            survey_features = {}
            survey_keys = [
                'survey_overspending_score', 'survey_debt_risk_score',
                'survey_savings_capacity', 'survey_anxiety_index',
                'behavioral_segment', 'segment_confidence', 
                'survey_completed', 'survey_completed_at', 'risk_level'
            ]
            
            for k, v in data.items():
                if k in survey_keys:
                    survey_features[k] = self._parse_feature_value(v)
            
            return survey_features if survey_features else None
            
        except Exception as e:
            logger.error(f"Error fetching survey features for user {user_id}: {e}")
            return None
    
    async def has_completed_survey(self, user_id: int) -> bool:
        """Check if user has completed the behavioral survey."""
        completed = await self.get_feature(user_id, "survey_completed")
        return completed == True or completed == "true"

    def _parse_feature_value(self, value: Any) -> Any:
        """Parse Redis feature value with JSON-first strategy."""
        if value is None:
            return None
        if not isinstance(value, str):
            return value

        try:
            return json.loads(value)
        except (json.JSONDecodeError, TypeError):
            lowered = value.strip().lower()
            if lowered == "true":
                return True
            if lowered == "false":
                return False
            try:
                return float(value)
            except (ValueError, TypeError):
                return value
