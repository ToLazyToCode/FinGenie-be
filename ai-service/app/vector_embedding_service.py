"""
FinGenie AI Service - Vector Embedding Service (pgvector)
"""
import asyncio
import json
import logging
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, List, Optional

import numpy as np

try:
    from psycopg2.pool import ThreadedConnectionPool
    _PSYCOPG2_IMPORT_ERROR: Optional[Exception] = None
except Exception as ex:
    ThreadedConnectionPool = None  # type: ignore[assignment]
    _PSYCOPG2_IMPORT_ERROR = ex

try:
    from sentence_transformers import SentenceTransformer
    _SENTENCE_TRANSFORMERS_IMPORT_ERROR: Optional[Exception] = None
except Exception as ex:
    SentenceTransformer = None  # type: ignore[assignment]
    _SENTENCE_TRANSFORMERS_IMPORT_ERROR = ex

from app.config import Settings

logger = logging.getLogger(__name__)

GLOBAL_FINANCIAL_INSIGHTS = [
    "Track daily expenses by category to detect overspending patterns early.",
    "Automate savings right after income arrives to improve savings consistency.",
    "Review high-volatility spending categories weekly to reduce impulse decisions.",
    "Compare weekday and weekend expenses to identify emotional spending behavior.",
]


@dataclass
class RetrievedContext:
    source_type: str
    source_id: Optional[int]
    content: str
    score: float
    metadata: Dict[str, Any]
    created_at: Optional[str]


class VectorEmbeddingService:
    """
    Handles text embedding + storage/retrieval in PostgreSQL pgvector.

    Java backend remains unaware of vector internals; this service is Python-only.
    """

    def __init__(self, settings: Settings):
        self.settings = settings
        self.table_name = self._sanitize_table_name(settings.vector_table)
        self.pool: Optional[Any] = None
        self.model: Optional[Any] = None
        self._enabled = False

    @property
    def enabled(self) -> bool:
        return self._enabled

    async def initialize(self):
        if not self.settings.vector_enabled:
            logger.info("Vector store disabled by configuration")
            self._enabled = False
            return

        await asyncio.to_thread(self._initialize_sync)

    async def close(self):
        await asyncio.to_thread(self._close_sync)

    async def index_text(
        self,
        user_id: int,
        source_type: str,
        source_id: Optional[int],
        content: str,
        metadata: Optional[Dict[str, Any]] = None,
    ):
        if not self._enabled:
            return
        if not content or not content.strip():
            return
        await asyncio.to_thread(
            self._index_text_sync,
            user_id,
            source_type,
            source_id,
            content.strip(),
            metadata or {},
        )

    async def retrieve_context(
        self,
        user_id: int,
        query: str,
        limit: Optional[int] = None,
    ) -> List[RetrievedContext]:
        if not self._enabled:
            return []
        if not query or not query.strip():
            return []

        normalized_limit = limit or self.settings.vector_top_k
        normalized_limit = max(1, min(normalized_limit, 20))
        return await asyncio.to_thread(
            self._retrieve_context_sync,
            user_id,
            query.strip(),
            normalized_limit,
        )

    def _initialize_sync(self):
        try:
            if _SENTENCE_TRANSFORMERS_IMPORT_ERROR is not None:
                logger.error(
                    "Vector embedding disabled: sentence-transformers import failed: %s",
                    _SENTENCE_TRANSFORMERS_IMPORT_ERROR,
                )
                self._enabled = False
                return
            if _PSYCOPG2_IMPORT_ERROR is not None:
                logger.error(
                    "Vector embedding disabled: psycopg2 import failed: %s",
                    _PSYCOPG2_IMPORT_ERROR,
                )
                self._enabled = False
                return

            logger.info(
                "Initializing vector embedding model: %s",
                self.settings.embedding_model_name,
            )
            self.model = SentenceTransformer(self.settings.embedding_model_name)

            self.pool = ThreadedConnectionPool(
                minconn=1,
                maxconn=5,
                dsn=self.settings.vector_db_url,
            )

            conn = self.pool.getconn()
            try:
                with conn.cursor() as cur:
                    cur.execute("CREATE EXTENSION IF NOT EXISTS vector")
                    cur.execute(
                        f"""
                        CREATE TABLE IF NOT EXISTS {self.table_name} (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            source_type VARCHAR(64) NOT NULL,
                            source_id BIGINT NULL,
                            content TEXT NOT NULL,
                            metadata JSONB NOT NULL DEFAULT '{{}}'::jsonb,
                            embedding vector({self.settings.vector_dimension}) NOT NULL,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                        )
                        """
                    )
                    cur.execute(
                        f"CREATE INDEX IF NOT EXISTS idx_{self.table_name}_user_id "
                        f"ON {self.table_name}(user_id)"
                    )
                    cur.execute(
                        f"CREATE INDEX IF NOT EXISTS idx_{self.table_name}_embedding "
                        f"ON {self.table_name} USING ivfflat (embedding vector_cosine_ops) "
                        f"WITH (lists = 100)"
                    )
                conn.commit()
            finally:
                self.pool.putconn(conn)

            self._seed_global_insights_sync()
            self._enabled = True
            logger.info("Vector embedding service initialized successfully")

        except Exception as ex:
            self._enabled = False
            logger.error("Vector embedding service failed to initialize: %s", ex, exc_info=True)

    def _close_sync(self):
        if self.pool:
            self.pool.closeall()
            self.pool = None
        self._enabled = False

    def _seed_global_insights_sync(self):
        if not self.pool:
            return

        conn = self.pool.getconn()
        try:
            with conn.cursor() as cur:
                cur.execute(
                    f"SELECT COUNT(*) FROM {self.table_name} "
                    "WHERE user_id = 0 AND source_type = 'financial_insight'"
                )
                existing = cur.fetchone()[0]
                if existing > 0:
                    return

                for insight in GLOBAL_FINANCIAL_INSIGHTS:
                    embedding = self._embed_text_sync(insight)
                    vector_literal = self._to_vector_literal(embedding)
                    cur.execute(
                        f"""
                        INSERT INTO {self.table_name}
                        (user_id, source_type, source_id, content, metadata, embedding)
                        VALUES (%s, %s, %s, %s, %s::jsonb, %s::vector)
                        """,
                        (
                            0,
                            "financial_insight",
                            None,
                            insight,
                            json.dumps({"seeded": True}),
                            vector_literal,
                        ),
                    )
            conn.commit()
        finally:
            self.pool.putconn(conn)

    def _index_text_sync(
        self,
        user_id: int,
        source_type: str,
        source_id: Optional[int],
        content: str,
        metadata: Dict[str, Any],
    ):
        if not self.pool:
            return

        embedding = self._embed_text_sync(content)
        vector_literal = self._to_vector_literal(embedding)

        conn = self.pool.getconn()
        try:
            with conn.cursor() as cur:
                cur.execute(
                    f"""
                    INSERT INTO {self.table_name}
                    (user_id, source_type, source_id, content, metadata, embedding)
                    VALUES (%s, %s, %s, %s, %s::jsonb, %s::vector)
                    """,
                    (
                        user_id,
                        source_type,
                        source_id,
                        content,
                        json.dumps(metadata),
                        vector_literal,
                    ),
                )
            conn.commit()
        finally:
            self.pool.putconn(conn)

    def _retrieve_context_sync(
        self,
        user_id: int,
        query: str,
        limit: int,
    ) -> List[RetrievedContext]:
        if not self.pool:
            return []

        query_embedding = self._embed_text_sync(query)
        vector_literal = self._to_vector_literal(query_embedding)
        candidate_limit = min(max(limit * 3, limit + 5), 60)

        conn = self.pool.getconn()
        try:
            with conn.cursor() as cur:
                cur.execute(
                    f"""
                    SELECT
                        user_id,
                        source_type,
                        source_id,
                        content,
                        metadata,
                        created_at,
                        (1 - (embedding <=> %s::vector)) AS score
                    FROM {self.table_name}
                    WHERE user_id = %s OR user_id = 0
                    ORDER BY embedding <=> %s::vector
                    LIMIT %s
                    """,
                    (vector_literal, user_id, vector_literal, candidate_limit),
                )
                rows = cur.fetchall()

            results: List[RetrievedContext] = []
            for row in rows:
                owner_user_id = row[0]
                metadata = row[4]
                if isinstance(metadata, str):
                    try:
                        metadata = json.loads(metadata)
                    except Exception:
                        metadata = {}
                if metadata is None:
                    metadata = {}
                if not isinstance(metadata, dict):
                    metadata = {}
                metadata.setdefault("owner_user_id", owner_user_id)

                created_at = row[5].isoformat() if row[5] else None
                score = float(row[6]) if row[6] is not None else 0.0
                results.append(
                    RetrievedContext(
                        source_type=row[1],
                        source_id=row[2],
                        content=row[3],
                        score=max(0.0, min(score, 1.0)),
                        metadata=metadata,
                        created_at=created_at,
                    )
                )
            return self._rerank_context(results, user_id, limit)
        finally:
            self.pool.putconn(conn)

    def _rerank_context(
        self,
        candidates: List[RetrievedContext],
        user_id: int,
        limit: int,
    ) -> List[RetrievedContext]:
        if not candidates:
            return []

        deduped: Dict[str, RetrievedContext] = {}
        for item in candidates:
            content = (item.content or "").strip()
            if not content:
                continue

            dedupe_key = f"{item.source_type}:{item.source_id}:{content[:120]}"
            previous = deduped.get(dedupe_key)
            if previous is None or item.score > previous.score:
                item.content = content
                deduped[dedupe_key] = item

        ranked: List[RetrievedContext] = []
        for item in deduped.values():
            owner_user_id = item.metadata.get("owner_user_id")
            owner_bonus = 0.08 if owner_user_id == user_id else 0.0
            source_bonus = {
                "chat_user_message": 0.08,
                "chat_ai_message": 0.04,
                "financial_insight": 0.0,
            }.get(item.source_type, 0.01)
            recency_bonus = self._recency_bonus(item.created_at)
            combined_score = max(0.0, min(item.score + owner_bonus + source_bonus + recency_bonus, 1.0))
            item.score = combined_score
            ranked.append(item)

        ranked.sort(key=lambda x: x.score, reverse=True)
        return ranked[:limit]

    @staticmethod
    def _recency_bonus(created_at: Optional[str]) -> float:
        if not created_at:
            return 0.0
        try:
            parsed = datetime.fromisoformat(created_at.replace("Z", "+00:00"))
            if parsed.tzinfo is None:
                parsed = parsed.replace(tzinfo=timezone.utc)
        except Exception:
            return 0.0

        now = datetime.now(timezone.utc)
        if parsed >= now - timedelta(days=7):
            return 0.04
        if parsed >= now - timedelta(days=30):
            return 0.02
        return 0.0

    def _embed_text_sync(self, text: str) -> np.ndarray:
        if not self.model:
            raise RuntimeError("Embedding model is not initialized")

        embedding = self.model.encode(text, normalize_embeddings=True)
        if len(embedding) != self.settings.vector_dimension:
            raise ValueError(
                f"Embedding dimension mismatch: expected {self.settings.vector_dimension}, "
                f"got {len(embedding)}"
            )
        return embedding

    @staticmethod
    def _to_vector_literal(values: np.ndarray) -> str:
        return "[" + ",".join(f"{float(v):.8f}" for v in values) + "]"

    @staticmethod
    def _sanitize_table_name(table_name: str) -> str:
        sanitized = "".join(ch for ch in table_name if ch.isalnum() or ch == "_")
        return sanitized or "ai_embeddings"
