#!/usr/bin/env bash
set -euo pipefail

server_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
set -a
source "$server_dir/.env"
set +a

db_host="${DB_HOST:-${POSTGRES_HOST:-}}"
db_port="${DB_PORT:-${POSTGRES_PORT:-5432}}"
db_name="${DB_NAME:-${POSTGRES_DB:-postgres}}"
db_user="${DB_USER:-postgres}"
db_password="${DB_PASSWORD:-${POSTGRES_PASSWORD:-}}"

if [[ -z "$db_host" || -z "$db_password" ]]; then
  echo "DB_HOST/DB_PASSWORD (or legacy POSTGRES_* equivalents) are required" >&2
  exit 1
fi

PGPASSWORD="$db_password" psql \
  -X -v ON_ERROR_STOP=1 \
  -h "$db_host" -p "$db_port" -U "$db_user" -d "$db_name" \
  -f "$server_dir/sql/001_create_archive_assistant_schema.sql"
