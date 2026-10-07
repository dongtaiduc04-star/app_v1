CREATE TABLE users (
  id BINARY(16) NOT NULL,
  username VARCHAR(30) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  email VARCHAR(254) COLLATE utf8mb4_0900_ai_ci NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
  failed_login_attempts INT UNSIGNED NOT NULL DEFAULT 0,
  locked_until TIMESTAMP(6) NULL,
  deleted_at TIMESTAMP(6) NULL,
  purge_after TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), UNIQUE KEY uk_users_username (username), UNIQUE KEY uk_users_email (email),
  KEY ix_users_status_purge (status, purge_after)
);
CREATE TABLE user_roles (
  user_id BINARY(16) NOT NULL, role_name VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, role_name), CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE password_credentials (
  user_id BINARY(16) NOT NULL, password_hash VARCHAR(255) NOT NULL, changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (user_id), CONSTRAINT fk_password_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE oauth_identities (
  id BINARY(16) NOT NULL, user_id BINARY(16) NOT NULL, provider VARCHAR(32) NOT NULL, provider_subject VARCHAR(255) NOT NULL,
  email VARCHAR(254) NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), UNIQUE KEY uk_oauth_provider_subject (provider, provider_subject), KEY ix_oauth_user (user_id),
  CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE refresh_sessions (
  id BINARY(16) NOT NULL, user_id BINARY(16) NOT NULL, token_family BINARY(16) NOT NULL, token_hash CHAR(64) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL, revoked_at TIMESTAMP(6) NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), UNIQUE KEY uk_refresh_token_hash (token_hash), KEY ix_refresh_user (user_id), KEY ix_refresh_expiry (expires_at),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE password_reset_tokens (
  id BINARY(16) NOT NULL, user_id BINARY(16) NOT NULL, token_hash CHAR(64) NOT NULL, expires_at TIMESTAMP(6) NOT NULL,
  used_at TIMESTAMP(6) NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id), UNIQUE KEY uk_reset_token_hash (token_hash), KEY ix_reset_user (user_id), KEY ix_reset_expiry (expires_at),
  CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
