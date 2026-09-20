BEGIN;
DELETE FROM archive_assistant.workspaces WHERE workspace_id = 'android-cli-e2e';
COMMIT;
