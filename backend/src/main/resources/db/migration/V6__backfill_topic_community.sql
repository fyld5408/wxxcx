-- 旧版后端不认识 province/city/community 三列，会以空值建主题：
-- 这些主题对所有小区都不可见。按发起人当前登记的门牌补回小区。
-- 同一小区内若已有同名主题，跳过该行（UPDATE IGNORE 正好承担这个作用）。
UPDATE IGNORE vote_topic t
    JOIN user_address a ON a.openid = t.openid
SET t.province = a.province, t.city = a.city, t.community = a.community
WHERE t.community = '';
