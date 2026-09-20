BEGIN;

INSERT INTO archive_assistant.workspaces (workspace_id, revision)
VALUES ('android-cli-e2e', 0)
ON CONFLICT (workspace_id) DO NOTHING;

DELETE FROM archive_assistant.knowledge_items WHERE workspace_id = 'android-cli-e2e';
DELETE FROM archive_assistant.topics WHERE workspace_id = 'android-cli-e2e';

INSERT INTO archive_assistant.topics
  (workspace_id, id, title, icon_name, icon_color, updated_at_epoch_millis)
VALUES
  ('android-cli-e2e', 'officials', '吏 · 名籍', 'folder-spark', '#5e5d59', 1715000000000),
  ('android-cli-e2e', 'treasury', '户 · 府库', 'folder-spark', '#5e5d59', 1715000000001),
  ('android-cli-e2e', 'rites', '礼 · 典章', 'folder-spark', '#5e5d59', 1715000000002),
  ('android-cli-e2e', 'military', '兵 · 行令', 'folder-spark', '#5e5d59', 1715000000003),
  ('android-cli-e2e', 'justice', '刑 · 稽核', 'folder-spark', '#5e5d59', 1715000000004),
  ('android-cli-e2e', 'works', '工 · 营造', 'folder-spark', '#5e5d59', 1715000000005);

INSERT INTO archive_assistant.knowledge_items
  (workspace_id, id, topic_id, content_type, title, summary, full_text,
   source_url, document_format, file_name, file_size, created_at_epoch_millis)
VALUES
  (
    'android-cli-e2e', 'android-cli-cloud-document', 'works', 'DOCUMENT',
    '云端联调验证文档', '来自 PostgreSQL 的 Android CLI 端到端测试数据',
    '若此内容能在 Android 应用中显示，说明 server、PostgreSQL 与客户端读取链路正常。',
    NULL, 'MARKDOWN', 'cloud-e2e.md', 96, 1760000000000
  ),
  (
    'android-cli-e2e', 'android-cli-cloud-article', 'rites', 'WEB_ARTICLE',
    '云端礼部测试文章', '用于验证不同内容类型的云端反序列化',
    'ArchiveAssistant server interaction test.',
    'https://example.com/archive-assistant-e2e', NULL, NULL, NULL, 1760000001000
  );

UPDATE archive_assistant.workspaces
SET revision = revision + 1, updated_at = now()
WHERE workspace_id = 'android-cli-e2e';

COMMIT;
