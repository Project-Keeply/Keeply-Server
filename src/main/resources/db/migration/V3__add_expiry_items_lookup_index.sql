CREATE INDEX idx_expiry_items_group_expire_id
ON expiry_items (group_id, expire_date ASC, id ASC);
