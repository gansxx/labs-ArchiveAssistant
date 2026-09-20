#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
set -a
source "$server_dir/.env"
set +a

exec uvicorn app:app \
  --app-dir "$server_dir" \
  --host "${ARCHIVE_API_HOST:-0.0.0.0}" \
  --port "${ARCHIVE_API_PORT:-8080}"
