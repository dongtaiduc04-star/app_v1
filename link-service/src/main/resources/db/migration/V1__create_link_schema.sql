CREATE TABLE profiles (
  id BINARY(16) NOT NULL, user_id BINARY(16) NOT NULL,
  username VARCHAR(30) COLLATE utf8mb4_0900_ai_ci NOT NULL, display_name VARCHAR(100) NOT NULL,
  bio VARCHAR(500) NULL, avatar_url VARCHAR(2048) NULL, status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
  version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), UNIQUE KEY uk_profiles_user (user_id), UNIQUE KEY uk_profiles_username (username), KEY ix_profiles_status (status)
);
CREATE TABLE links (
  id BINARY(16) NOT NULL, profile_id BINARY(16) NOT NULL, title VARCHAR(120) NOT NULL,
  destination_url VARCHAR(2048) NOT NULL, icon VARCHAR(100) NULL, position SMALLINT UNSIGNED NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE, click_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), UNIQUE KEY uk_links_profile_position (profile_id, position), KEY ix_links_profile_enabled (profile_id, enabled),
  CONSTRAINT fk_links_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE
);
