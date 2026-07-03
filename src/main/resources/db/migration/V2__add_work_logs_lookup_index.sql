CREATE INDEX idx_work_logs_group_created_id
ON work_logs (group_id, created_at DESC, id DESC);
