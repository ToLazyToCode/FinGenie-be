"""
FinGenie AI Service - Configuration
"""
from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""
    
    # Service Info
    service_name: str = "fingenie-ai-service"
    environment: str = "development"
    debug: bool = True
    
    # API
    api_host: str = "0.0.0.0"
    api_port: int = 8000
    
    # Kafka
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group: str = "fingenie-ai-consumers"
    kafka_transaction_topic: str = "transaction-events"
    kafka_user_topic: str = "user-events"
    kafka_survey_topic: str = "survey-events"
    kafka_prediction_topic: str = "model-predictions"
    kafka_feature_topic: str = "feature-updates"
    
    # Redis
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_db: int = 0
    redis_password: str = ""
    
    # MLflow
    mlflow_tracking_uri: str = "http://localhost:5000"
    mlflow_experiment_name: str = "fingenie-spending-predictor"
    
    # Model
    ai_model_version: str = "v1.0.0"
    ai_model_path: str = "/app/models"
    ai_fallback_enabled: bool = True
    ai_inference_timeout_ms: int = 300
    
    # Feature Store
    feature_cache_ttl_seconds: int = 86400
    feature_version: str = "v1"
    backend_url: str = "http://host.docker.internal:8080"

    # Vector Store (pgvector)
    vector_enabled: bool = True
    vector_db_url: str = "postgresql://fingenie:change_me@postgres:5432/fingenie"
    vector_table: str = "ai_embeddings"
    vector_dimension: int = 384
    vector_top_k: int = 5
    embedding_model_name: str = "sentence-transformers/all-MiniLM-L6-v2"

    # LLM (RAG chat)
#     ollama_base_url: str = "http://ollama:11434"
#     ollama_model: str = "llama3.2"
#     llm_timeout_seconds: int = 120
#     llm_failure_cooldown_seconds: int = 300

    # LLM (Gemini)
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.5-flash"
    llm_timeout_seconds: int = 120
    llm_failure_cooldown_seconds: int = 300
    
    # Monitoring
    log_level: str = "INFO"
    metrics_enabled: bool = True
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()
