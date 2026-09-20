from __future__ import annotations

import hmac
import os
import re
from contextlib import asynccontextmanager
from typing import Annotated, Literal

from fastapi import Depends, FastAPI, Header, HTTPException, Response, status
from pydantic import BaseModel, ConfigDict, Field
from psycopg.rows import dict_row
from psycopg_pool import ConnectionPool

SCHEMA = "archive_assistant"
WORKSPACE_PATTERN = re.compile(r"^[A-Za-z0-9._-]{1,128}$")


def env_value(primary: str, legacy: str | None = None, default: str | None = None) -> str:
    value = os.getenv(primary) or (os.getenv(legacy) if legacy else None) or default
    if value is None:
        raise RuntimeError(f"Missing required environment variable: {primary}")
    return value


def connection_string() -> str:
    host = env_value("DB_HOST", "POSTGRES_HOST")
    port = env_value("DB_PORT", "POSTGRES_PORT", "5432")
    database = env_value("DB_NAME", "POSTGRES_DB", "postgres")
    user = env_value("DB_USER", default="postgres")
    password = env_value("DB_PASSWORD", "POSTGRES_PASSWORD")
    return f"host={host} port={port} dbname={database} user={user} password={password}"


class Topic(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: str = Field(min_length=1, max_length=128)
    title: str = Field(min_length=1, max_length=500)
    iconName: str
    iconColor: str = Field(pattern=r"^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")
    updatedAtEpochMillis: int = Field(ge=0)


class KnowledgeItem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: str = Field(min_length=1, max_length=128)
    topicId: str = Field(min_length=1, max_length=128)
    contentType: Literal["WEB_ARTICLE", "IMAGE_SCREENSHOT", "DOCUMENT"]
    title: str = Field(min_length=1, max_length=1000)
    summary: str = ""
    fullText: str = ""
    sourceUrl: str | None = None
    imageResName: str | None = None
    documentFormat: Literal["PDF", "MARKDOWN", "TXT", "DOCX", "UNKNOWN"] | None = None
    fileName: str | None = None
    fileSize: int | None = Field(default=None, ge=0)
    createdAtEpochMillis: int = Field(ge=0)


class SnapshotWrite(BaseModel):
    model_config = ConfigDict(extra="forbid")

    topics: list[Topic]
    items: list[KnowledgeItem]


class Snapshot(SnapshotWrite):
    revision: int = Field(ge=0)


pool: ConnectionPool | None = None


@asynccontextmanager
async def lifespan(_: FastAPI):
    global pool
    pool = ConnectionPool(connection_string(), min_size=1, max_size=8, open=True)
    pool.wait()
    yield
    pool.close()
    pool = None


app = FastAPI(title="ArchiveAssistant Cloud Data API", version="1.0.0", lifespan=lifespan)


def require_api_key(authorization: Annotated[str | None, Header()] = None) -> None:
    expected = env_value("ARCHIVE_API_KEY")
    scheme, _, supplied = (authorization or "").partition(" ")
    if scheme.lower() != "bearer" or not hmac.compare_digest(supplied, expected):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid API key")


def checked_workspace_id(workspace_id: str) -> str:
    if not WORKSPACE_PATTERN.fullmatch(workspace_id):
        raise HTTPException(status_code=422, detail="Invalid workspace id")
    return workspace_id


def database_pool() -> ConnectionPool:
    if pool is None:
        raise HTTPException(status_code=503, detail="Database pool is not ready")
    return pool


@app.get("/health")
def health() -> dict[str, str]:
    with database_pool().connection() as connection:
        connection.execute("SELECT 1")
    return {"status": "ok"}


@app.get(
    "/v1/workspaces/{workspace_id}/snapshot",
    response_model=Snapshot,
    dependencies=[Depends(require_api_key)],
)
def get_snapshot(workspace_id: str) -> Snapshot:
    workspace_id = checked_workspace_id(workspace_id)
    with database_pool().connection() as connection:
        with connection.cursor(row_factory=dict_row) as cursor:
            cursor.execute(
                f"SELECT revision FROM {SCHEMA}.workspaces WHERE workspace_id = %s",
                (workspace_id,),
            )
            workspace = cursor.fetchone()
            if workspace is None:
                return Snapshot(revision=0, topics=[], items=[])
            cursor.execute(
                f"""
                SELECT id, title, icon_name AS "iconName", icon_color AS "iconColor",
                       updated_at_epoch_millis AS "updatedAtEpochMillis"
                FROM {SCHEMA}.topics
                WHERE workspace_id = %s
                ORDER BY updated_at_epoch_millis, id
                """,
                (workspace_id,),
            )
            topics = cursor.fetchall()
            cursor.execute(
                f"""
                SELECT id, topic_id AS "topicId", content_type AS "contentType", title,
                       summary, full_text AS "fullText", source_url AS "sourceUrl",
                       image_res_name AS "imageResName", document_format AS "documentFormat",
                       file_name AS "fileName", file_size AS "fileSize",
                       created_at_epoch_millis AS "createdAtEpochMillis"
                FROM {SCHEMA}.knowledge_items
                WHERE workspace_id = %s
                ORDER BY created_at_epoch_millis, id
                """,
                (workspace_id,),
            )
            items = cursor.fetchall()
    return Snapshot(revision=workspace["revision"], topics=topics, items=items)


@app.put(
    "/v1/workspaces/{workspace_id}/snapshot",
    response_model=Snapshot,
    dependencies=[Depends(require_api_key)],
)
def put_snapshot(workspace_id: str, snapshot: SnapshotWrite, response: Response) -> Snapshot:
    workspace_id = checked_workspace_id(workspace_id)
    topic_ids = [topic.id for topic in snapshot.topics]
    item_ids = [item.id for item in snapshot.items]
    if len(topic_ids) != len(set(topic_ids)) or len(item_ids) != len(set(item_ids)):
        raise HTTPException(status_code=422, detail="Topic and item ids must be unique")
    unknown_topic_ids = {item.topicId for item in snapshot.items} - set(topic_ids)
    if unknown_topic_ids:
        raise HTTPException(
            status_code=422,
            detail=f"Items reference unknown topics: {sorted(unknown_topic_ids)}",
        )

    with database_pool().connection() as connection:
        with connection.transaction():
            with connection.cursor(row_factory=dict_row) as cursor:
                cursor.execute(
                    f"""
                    INSERT INTO {SCHEMA}.workspaces (workspace_id)
                    VALUES (%s)
                    ON CONFLICT (workspace_id) DO NOTHING
                    """,
                    (workspace_id,),
                )
                cursor.execute(
                    f"SELECT revision FROM {SCHEMA}.workspaces WHERE workspace_id = %s FOR UPDATE",
                    (workspace_id,),
                )
                cursor.execute(
                    f"DELETE FROM {SCHEMA}.knowledge_items WHERE workspace_id = %s",
                    (workspace_id,),
                )
                cursor.execute(
                    f"DELETE FROM {SCHEMA}.topics WHERE workspace_id = %s",
                    (workspace_id,),
                )
                cursor.executemany(
                    f"""
                    INSERT INTO {SCHEMA}.topics
                      (workspace_id, id, title, icon_name, icon_color, updated_at_epoch_millis)
                    VALUES (%s, %s, %s, %s, %s, %s)
                    """,
                    [
                        (
                            workspace_id,
                            topic.id,
                            topic.title,
                            topic.iconName,
                            topic.iconColor,
                            topic.updatedAtEpochMillis,
                        )
                        for topic in snapshot.topics
                    ],
                )
                cursor.executemany(
                    f"""
                    INSERT INTO {SCHEMA}.knowledge_items
                      (workspace_id, id, topic_id, content_type, title, summary, full_text,
                       source_url, image_res_name, document_format, file_name, file_size,
                       created_at_epoch_millis)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """,
                    [
                        (
                            workspace_id,
                            item.id,
                            item.topicId,
                            item.contentType,
                            item.title,
                            item.summary,
                            item.fullText,
                            item.sourceUrl,
                            item.imageResName,
                            item.documentFormat,
                            item.fileName,
                            item.fileSize,
                            item.createdAtEpochMillis,
                        )
                        for item in snapshot.items
                    ],
                )
                cursor.execute(
                    f"""
                    UPDATE {SCHEMA}.workspaces
                    SET revision = revision + 1, updated_at = now()
                    WHERE workspace_id = %s
                    RETURNING revision
                    """,
                    (workspace_id,),
                )
                revision = cursor.fetchone()["revision"]
    response.headers["ETag"] = f'"{revision}"'
    return Snapshot(revision=revision, topics=snapshot.topics, items=snapshot.items)
