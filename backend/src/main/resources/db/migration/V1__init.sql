CREATE TABLE IF NOT EXISTS app_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    openid      VARCHAR(64)     NOT NULL,
    nickname    VARCHAR(64)     NULL,
    avatar_url  VARCHAR(512)    NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_openid (openid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
