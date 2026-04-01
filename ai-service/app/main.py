"""
FinGenie AI Service - Main FastAPI Application
"""
import asyncio
import logging
from contextlib import asynccontextmanager
from typing import Optional, List, Dict, Any

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, ConfigDict, Field
from prometheus_client import Counter, Histogram, generate_latest, CONTENT_TYPE_LATEST
from starlette.responses import Response

from app.config import get_settings
from app.inference_engine import InferenceEngine
from app.feature_client import FeatureClient
from app.kafka_consumer import KafkaConsumerService
from app.kafka_producer import KafkaProducerService
from app.rag_chat_service import RAGChatService
from app.vector_embedding_service import VectorEmbeddingService
from app.insight_engine import InsightEngine
from app.saving_plan_advisor import SavingPlanAdvisor

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

settings = get_settings()

# Prometheus metrics
PREDICTION_REQUESTS = Counter(
    'fingenie_prediction_requests_total',
    'Total prediction requests',
    ['status', 'fallback']
)
PREDICTION_LATENCY = Histogram(
    'fingenie_prediction_latency_seconds',
    'Prediction latency in seconds',
    buckets=[0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0]
)

# Global services
inference_engine: Optional[InferenceEngine] = None
feature_client: Optional[FeatureClient] = None
kafka_producer: Optional[KafkaProducerService] = None
kafka_consumer: Optional[KafkaConsumerService] = None
vector_embedding_service: Optional[VectorEmbeddingService] = None
rag_chat_service: Optional[RAGChatService] = None
insight_engine: Optional[InsightEngine] = None
saving_plan_advisor: Optional[SavingPlanAdvisor] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager."""
    global inference_engine, feature_client, kafka_producer, kafka_consumer
    global vector_embedding_service, rag_chat_service, insight_engine, saving_plan_advisor
    
    logger.info("Starting FinGenie AI Service...")
    
    # Initialize services
    feature_client = FeatureClient(settings)
    await feature_client.connect()
    
    inference_engine = InferenceEngine(settings, feature_client)
    await inference_engine.load_model()
    
    kafka_producer = KafkaProducerService(settings)
    await kafka_producer.start()

    vector_embedding_service = VectorEmbeddingService(settings)
    await vector_embedding_service.initialize()
    rag_chat_service = RAGChatService(settings, vector_embedding_service)
    insight_engine = InsightEngine(settings, feature_client)
    saving_plan_advisor = SavingPlanAdvisor(settings)
    
    kafka_consumer = KafkaConsumerService(
        settings=settings,
        inference_engine=inference_engine,
        kafka_producer=kafka_producer,
        feature_client=feature_client,
    )
    asyncio.create_task(kafka_consumer.start())
    
    logger.info("FinGenie AI Service started successfully")
    
    yield
    
    # Cleanup
    logger.info("Shutting down FinGenie AI Service...")
    if kafka_consumer:
        await kafka_consumer.stop()
    if kafka_producer:
        await kafka_producer.stop()
    if vector_embedding_service:
        await vector_embedding_service.close()
    if feature_client:
        await feature_client.disconnect()
    logger.info("FinGenie AI Service shutdown complete")


app = FastAPI(
    title="FinGenie AI Service",
    description="AI prediction service for FinGenie fintech application",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============================================
# Request/Response Models
# ============================================

class APIBaseModel(BaseModel):
    # Allow fields like model_name/model_version without Pydantic protected namespace warnings.
    model_config = ConfigDict(protected_namespaces=())


class PredictionRequest(APIBaseModel):
    """Request model for spending prediction."""
    user_id: int = Field(..., description="User ID")
    correlation_id: Optional[str] = Field(None, description="Correlation ID for tracing")


class PredictionResponse(APIBaseModel):
    """Response model for spending prediction."""
    user_id: int
    predicted_amount: float
    predicted_category: Optional[str] = None
    confidence: float
    risk_score: float
    model_version: str
    feature_hash: str
    explanation: dict
    inference_latency_ms: int
    fallback_used: bool = False


class HealthResponse(APIBaseModel):
    """Health check response."""
    status: str
    service: str
    version: str
    model_loaded: bool
    kafka_connected: bool
    redis_connected: bool


class ModelInfoResponse(APIBaseModel):
    """Model information response."""
    model_name: str
    model_version: str
    feature_version: str
    loaded_at: str
    prediction_count: int


class ChatRequest(APIBaseModel):
    """Request model for RAG chat."""
    accountId: int = Field(..., description="User/Account ID")
    conversationId: Optional[int] = Field(None, description="Conversation ID")
    message: str = Field(..., min_length=1, description="User message")
    context: Optional[str] = Field(None, description="Optional user-provided context")
    language: Optional[str] = Field(None, description="Preferred language code (en/vi)")
    startNewConversation: bool = Field(False, description="Start a new conversation")


class ChatResponse(APIBaseModel):
    """Response model for chat endpoint."""
    conversationId: int
    userMessage: dict
    aiMessage: dict
    suggestions: List[str] = []
    detectedIntent: str
    retrievedContext: List[dict] = []


class RetrieveContextRequest(APIBaseModel):
    """Request model for retrieval endpoint."""
    userId: int = Field(..., description="User ID")
    query: str = Field(..., min_length=1, description="Query text")
    topK: int = Field(5, ge=1, le=20, description="Top K results")


class RetrieveContextResponse(APIBaseModel):
    """Response model for retrieval endpoint."""
    userId: int
    query: str
    items: List[dict]


class InsightRequest(APIBaseModel):
    """Request model for insight endpoint."""
    accountId: Optional[int] = None
    userId: Optional[int] = None
    insightType: str = "FINANCIAL_SUMMARY"
    message: Optional[str] = None
    featureSnapshot: Optional[Dict[str, Any]] = None
    recentTransactions: Optional[List[Dict[str, Any]]] = None
    userSegment: Optional[str] = None


class SavingPlanAdviceRequest(APIBaseModel):
    """Request model for saving plan advisor endpoint."""
    saving_capacity: float = Field(..., description="Deterministic saving capacity")
    targets: List[Dict[str, Any]] = Field(default_factory=list)
    allocations: List[Dict[str, Any]] = Field(default_factory=list)
    recommendations: List[Dict[str, Any]] = Field(default_factory=list)
    category_spend: List[Dict[str, Any]] = Field(default_factory=list)
    app_language: Optional[str] = Field(None, description="Preferred app language code (en/vi)")
    preferred_response_language: Optional[str] = Field(None, description="Preferred response language label")
    response_language_strict: Optional[str] = Field(None, description="Strict language rule for response")


class SavingPlanAdviceResponse(APIBaseModel):
    """Response model for saving plan advisor endpoint."""
    short_summary: str
    actionable_suggestions: List[str]
    risk_warnings: List[str]
    friendly_tone: str


# ============================================
# API Endpoints
# ============================================

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    return HealthResponse(
        status="healthy",
        service=settings.service_name,
        version=settings.ai_model_version,
        model_loaded=inference_engine.is_model_loaded() if inference_engine else False,
        kafka_connected=kafka_producer.is_connected() if kafka_producer else False,
        redis_connected=await feature_client.ping() if feature_client else False
    )


@app.get("/metrics")
async def metrics():
    """Prometheus metrics endpoint."""
    return Response(
        content=generate_latest(),
        media_type=CONTENT_TYPE_LATEST
    )


@app.get("/model/info", response_model=ModelInfoResponse)
async def get_model_info():
    """Get current model information."""
    if not inference_engine:
        raise HTTPException(status_code=503, detail="Inference engine not initialized")
    
    return inference_engine.get_model_info()


@app.post("/predict", response_model=PredictionResponse)
async def predict(request: PredictionRequest, background_tasks: BackgroundTasks):
    """
    Make a spending prediction for a user.
    
    This endpoint:
    1. Fetches user features from Redis
    2. Runs inference using the loaded model
    3. Returns prediction with explanation
    4. Publishes result to Kafka (async)
    """
    import time
    start_time = time.time()
    
    if not inference_engine:
        raise HTTPException(status_code=503, detail="Inference engine not initialized")
    
    try:
        with PREDICTION_LATENCY.time():
            result = await inference_engine.predict(
                user_id=request.user_id,
                correlation_id=request.correlation_id
            )
        
        latency_ms = int((time.time() - start_time) * 1000)
        result["inference_latency_ms"] = latency_ms
        
        # Publish to Kafka in background
        if kafka_producer:
            background_tasks.add_task(
                kafka_producer.publish_prediction,
                result,
                request.correlation_id
            )
        
        PREDICTION_REQUESTS.labels(status="success", fallback=str(result.get("fallback_used", False))).inc()
        
        return PredictionResponse(**result)
    
    except Exception as e:
        PREDICTION_REQUESTS.labels(status="error", fallback="false").inc()
        logger.error(f"Prediction failed for user {request.user_id}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/ai/retrieve-context", response_model=RetrieveContextResponse)
async def retrieve_context(request: RetrieveContextRequest):
    """
    Retrieve semantically similar context for a user query from pgvector store.
    """
    if not rag_chat_service:
        raise HTTPException(status_code=503, detail="RAG service not initialized")

    items = await rag_chat_service.retrieve_context(
        user_id=request.userId,
        query=request.query,
        top_k=request.topK,
    )
    return RetrieveContextResponse(
        userId=request.userId,
        query=request.query,
        items=items,
    )


@app.post("/ai/chat", response_model=ChatResponse)
async def ai_chat(request: ChatRequest):
    """
    Chat endpoint using retrieval-augmented generation.
    """
    if not rag_chat_service:
        raise HTTPException(status_code=503, detail="RAG service not initialized")

    try:
        result = await rag_chat_service.chat(
            account_id=request.accountId,
            message=request.message,
            conversation_id=request.conversationId,
            context=request.context,
            language_code=request.language,
        )
        return ChatResponse(**result)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex


@app.post("/ai/insight")
async def ai_insight(request: InsightRequest):
    """
    Generate personalized financial insights from behavioral features and patterns.
    """
    if not insight_engine:
        raise HTTPException(status_code=503, detail="Insight engine not initialized")

    user_id = request.accountId or request.userId
    if user_id is None:
        raise HTTPException(status_code=400, detail="accountId or userId is required")

    return await insight_engine.generate_insight(
        user_id=user_id,
        insight_type=request.insightType,
        message=request.message,
        feature_snapshot=request.featureSnapshot,
        recent_transactions=request.recentTransactions,
        user_segment=request.userSegment,
    )


@app.post("/ai/saving-plan/advice", response_model=SavingPlanAdviceResponse)
async def saving_plan_advice(request: SavingPlanAdviceRequest):
    """
    Generate explanation-only advice for monthly saving plan.
    Numeric allocations are never modified by this endpoint.
    """
    if not saving_plan_advisor:
        raise HTTPException(status_code=503, detail="Saving plan advisor not initialized")

    result = await saving_plan_advisor.generate_advice(
        saving_capacity=request.saving_capacity,
        targets=request.targets,
        allocations=request.allocations,
        recommendations=request.recommendations,
        category_spend=request.category_spend,
        language_code=request.app_language,
    )
    return SavingPlanAdviceResponse(**result)


@app.post("/model/reload")
async def reload_model():
    """Reload the model (for hot-swapping)."""
    if not inference_engine:
        raise HTTPException(status_code=503, detail="Inference engine not initialized")
    
    try:
        await inference_engine.load_model()
        return {"status": "success", "message": "Model reloaded successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to reload model: {e}")


@app.get("/features/{user_id}")
async def get_user_features(user_id: int):
    """Get cached features for a user (debugging endpoint)."""
    if not feature_client:
        raise HTTPException(status_code=503, detail="Feature client not initialized")
    
    features = await feature_client.get_features(user_id)
    if not features:
        raise HTTPException(status_code=404, detail=f"No features found for user {user_id}")
    
    return features


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.api_host,
        port=settings.api_port,
        reload=settings.debug
    )
