-- 同一门牌（省/市/小区/栋/单元/房号）只允许一个业主登记
DELETE u1 FROM user_address u1
    JOIN user_address u2
        ON u1.province = u2.province
       AND u1.city = u2.city
       AND u1.community = u2.community
       AND u1.building = u2.building
       AND u1.unit = u2.unit
       AND u1.room_no = u2.room_no
       AND u1.id < u2.id;

ALTER TABLE user_address
    ADD UNIQUE KEY uk_address (province, city, community, building, unit, room_no);
