CREATE INDEX idx_notices_group_created_id
ON notices (group_id, created_at DESC, id DESC);

CREATE INDEX idx_notices_group_tag_created_id
ON notices (group_id, tag, created_at DESC, id DESC);
