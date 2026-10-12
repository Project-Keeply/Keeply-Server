-- 기존 토큰에는 sessionId claim이 없으므로 모든 Refresh 세션을 종료한다.
-- 삭제한 세션은 롤백으로 복원되지 않으며, 사용자는 재로그인해야 한다.
DELETE FROM refresh_token;

ALTER TABLE refresh_token
ADD COLUMN session_id VARCHAR(36) NOT NULL;
