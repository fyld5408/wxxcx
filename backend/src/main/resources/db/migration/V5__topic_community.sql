-- 主题归属小区：把发起人的省/市/小区落到主题行上，标题按小区内唯一
ALTER TABLE vote_topic
    ADD COLUMN province   VARCHAR(32) NOT NULL DEFAULT '',
    ADD COLUMN city       VARCHAR(32) NOT NULL DEFAULT '',
    ADD COLUMN community  VARCHAR(64) NOT NULL DEFAULT '';

UPDATE vote_topic t
    JOIN user_address a ON a.openid = t.openid
SET t.province = a.province, t.city = a.city, t.community = a.community;

ALTER TABLE vote_topic
    DROP INDEX uk_title,
    ADD UNIQUE KEY uk_community_title (title, province, city, community);
