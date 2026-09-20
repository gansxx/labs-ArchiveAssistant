# ArchiveAssistant cloud data service

This service stores the app's `Topic` and `KnowledgeItem` snapshots in the isolated PostgreSQL
schema `archive_assistant`. Device-only AI settings and API credentials remain in Android
DataStore and are never uploaded.

## Setup

```bash
cd server
cp .env.example .env
# Fill in DB_* and ARCHIVE_API_KEY.
./migrate.sh
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
./run.sh
```

`migrate.sh` accepts the current `DB_*` names and the legacy `POSTGRES_*` names. Migration 001 is
idempotent. The rollback SQL is intentionally separate because it deletes all cloud data.

## API

- `GET /health`
- `GET /v1/workspaces/{workspace_id}/snapshot`
- `PUT /v1/workspaces/{workspace_id}/snapshot`

All `/v1` requests require `Authorization: Bearer <ARCHIVE_API_KEY>`. Snapshot replacement is
transactional: readers observe either the old complete snapshot or the new complete snapshot.

## Android selection

Local DataStore remains the default. To build with the cloud source, provide Gradle properties or
environment variables:

```properties
ARCHIVE_DATA_BACKEND=CLOUD
ARCHIVE_CLOUD_BASE_URL=https://archive-api.example.com
ARCHIVE_CLOUD_WORKSPACE_ID=my-device-or-account
ARCHIVE_CLOUD_API_KEY=replace-with-deployment-token
```

Do not commit these values. For a public production client, replace the shared deployment key with
short-lived per-user tokens at the API gateway; the current bearer key is suitable for a private
deployment.
