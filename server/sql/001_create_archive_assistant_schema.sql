BEGIN;

CREATE SCHEMA IF NOT EXISTS archive_assistant;

CREATE TABLE IF NOT EXISTS archive_assistant.workspaces (
  workspace_id text PRIMARY KEY,
  revision bigint NOT NULL DEFAULT 0 CHECK (revision >= 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CHECK (workspace_id ~ '^[A-Za-z0-9._-]{1,128}$')
);

CREATE TABLE IF NOT EXISTS archive_assistant.topics (
  workspace_id text NOT NULL,
  id text NOT NULL,
  title text NOT NULL,
  icon_name text NOT NULL,
  icon_color text NOT NULL,
  updated_at_epoch_millis bigint NOT NULL CHECK (updated_at_epoch_millis >= 0),
  PRIMARY KEY (workspace_id, id),
  CONSTRAINT topics_workspace_fk
    FOREIGN KEY (workspace_id)
    REFERENCES archive_assistant.workspaces (workspace_id)
    ON DELETE CASCADE,
  CHECK (length(id) BETWEEN 1 AND 128),
  CHECK (length(title) BETWEEN 1 AND 500),
  CHECK (icon_color ~ '^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$')
);

CREATE TABLE IF NOT EXISTS archive_assistant.knowledge_items (
  workspace_id text NOT NULL,
  id text NOT NULL,
  topic_id text NOT NULL,
  content_type text NOT NULL,
  title text NOT NULL,
  summary text NOT NULL DEFAULT '',
  full_text text NOT NULL DEFAULT '',
  source_url text,
  image_res_name text,
  document_format text,
  file_name text,
  file_size bigint,
  created_at_epoch_millis bigint NOT NULL CHECK (created_at_epoch_millis >= 0),
  PRIMARY KEY (workspace_id, id),
  CONSTRAINT knowledge_items_topic_fk
    FOREIGN KEY (workspace_id, topic_id)
    REFERENCES archive_assistant.topics (workspace_id, id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CHECK (length(id) BETWEEN 1 AND 128),
  CHECK (length(title) BETWEEN 1 AND 1000),
  CHECK (content_type IN ('WEB_ARTICLE', 'IMAGE_SCREENSHOT', 'DOCUMENT')),
  CHECK (document_format IS NULL OR document_format IN ('PDF', 'MARKDOWN', 'TXT', 'DOCX', 'UNKNOWN')),
  CHECK (file_size IS NULL OR file_size >= 0)
);

CREATE INDEX IF NOT EXISTS knowledge_items_workspace_created_idx
  ON archive_assistant.knowledge_items (workspace_id, created_at_epoch_millis DESC);

CREATE INDEX IF NOT EXISTS knowledge_items_workspace_topic_idx
  ON archive_assistant.knowledge_items (workspace_id, topic_id);

COMMENT ON SCHEMA archive_assistant IS
  'ArchiveAssistant cloud snapshots. AI credentials and device-only settings are intentionally excluded.';
COMMENT ON TABLE archive_assistant.workspaces IS
  'Logical client/user boundary and monotonically increasing snapshot revision.';
COMMENT ON TABLE archive_assistant.topics IS
  'Cloud representation of the Android Topic model.';
COMMENT ON TABLE archive_assistant.knowledge_items IS
  'Cloud representation of the Android KnowledgeItem model.';

COMMIT;
