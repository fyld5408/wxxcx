-- 一个用户只保留一条登记信息：去掉历史重复行，再把 openid 变成唯一键
DELETE u1 FROM user_address u1
    JOIN user_address u2 ON u1.openid = u2.openid AND u1.id < u2.id;

ALTER TABLE user_address
    DROP INDEX idx_openid,
    ADD UNIQUE KEY uk_openid (openid);

CREATE TABLE IF NOT EXISTS vote_topic (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title      VARCHAR(64)     NOT NULL,
    openid     VARCHAR(64)     NOT NULL,
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_title (title)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS vote_record (
    id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    topic_id BIGINT UNSIGNED NOT NULL,
    openid   VARCHAR(64)     NOT NULL,
    choice   VARCHAR(8)      NOT NULL,
    address  VARCHAR(64)     NOT NULL,
    voted_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_topic_openid (topic_id, openid),
    KEY idx_topic (topic_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
