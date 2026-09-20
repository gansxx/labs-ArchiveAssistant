-- Destructive rollback for migration 001. Run explicitly only when all cloud data may be deleted.
BEGIN;
DROP SCHEMA IF EXISTS archive_assistant CASCADE;
COMMIT;
