-- ============================================================
-- V1: 초기 스키마
-- ============================================================

-- users
CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  kakao_id VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  profile_image_url VARCHAR(255),
  is_name_customized BIT(1) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6),
  deleted_at DATETIME(6),
  PRIMARY KEY (id),
  CONSTRAINT uq_users_kakao_id UNIQUE (kakao_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- refresh_token
CREATE TABLE refresh_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token VARCHAR(255) NOT NULL,
  expiry_date DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_refresh_token_user_id UNIQUE (user_id),
  CONSTRAINT uq_refresh_token_token UNIQUE (token),
  CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- groups
CREATE TABLE `groups` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  store_brand VARCHAR(255) NOT NULL,
  invite_code VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_groups_invite_code UNIQUE (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- group_members
-- deleted_flag: 소프트 삭제 상태 파생 컬럼 (활성=0, 삭제=NULL)
-- (user_id, deleted_flag) 합성 UNIQUE: 활성 멤버는 user_id당 1행만 허용, 삭제 이력은 무제한 누적 가능
--   → MySQL UNIQUE가 NULL 중복을 허용하는 성질 활용
-- (group_id, user_id) 복합 UNIQUE: notices/work_logs/expiry_items의 복합 FK 참조 대상
CREATE TABLE group_members (
  id BIGINT NOT NULL AUTO_INCREMENT,
  group_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(255) NOT NULL,
  joined_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6),
  deleted_flag TINYINT GENERATED ALWAYS AS (IF(deleted_at IS NULL, 0, NULL)) VIRTUAL,
  PRIMARY KEY (id),
  CONSTRAINT uq_group_members_user_id_active UNIQUE (user_id, deleted_flag),
  CONSTRAINT uq_group_members_group_user UNIQUE (group_id, user_id),
  CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES `groups` (id),
  CONSTRAINT fk_group_members_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- notices
CREATE TABLE notices (
  id BIGINT NOT NULL AUTO_INCREMENT,
  group_id BIGINT NOT NULL,
  author_user_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  tag VARCHAR(255) NOT NULL,
  image_url VARCHAR(255),
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_notices_author_member
    FOREIGN KEY (group_id, author_user_id)
    REFERENCES group_members (group_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- work_logs
CREATE TABLE work_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  group_id BIGINT NOT NULL,
  author_user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_work_logs_author_member
    FOREIGN KEY (group_id, author_user_id)
    REFERENCES group_members (group_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- expiry_items
CREATE TABLE expiry_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  group_id BIGINT NOT NULL,
  author_user_id BIGINT NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  expire_date DATE NOT NULL,
  image_url VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_expiry_items_author_member
    FOREIGN KEY (group_id, author_user_id)
    REFERENCES group_members (group_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
