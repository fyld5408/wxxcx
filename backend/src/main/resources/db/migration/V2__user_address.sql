CREATE TABLE IF NOT EXISTS user_address (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    openid      VARCHAR(64)  NOT NULL,
    province    VARCHAR(32)  NOT NULL,
    city        VARCHAR(32)  NOT NULL,
    community   VARCHAR(64)  NOT NULL,
    building    VARCHAR(32)  NOT NULL,
    unit        VARCHAR(32)  NOT NULL,
    room_no     VARCHAR(32)  NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_openid (openid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
