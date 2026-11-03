package com.fyld.wxxcx.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final RowMapper<AppUser> MAPPER = (rs, rowNum) -> new AppUser(
            rs.getLong("id"),
            rs.getString("openid"),
            rs.getString("nickname"),
            rs.getString("avatar_url"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("last_seen_at").toLocalDateTime());

    private final JdbcTemplate jdbc;

    public UserService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AppUser touch(String openid) {
        jdbc.update("INSERT INTO app_user (openid) VALUES (?) "
                + "ON DUPLICATE KEY UPDATE last_seen_at = CURRENT_TIMESTAMP", openid);
        return jdbc.queryForObject(
                "SELECT id, openid, nickname, avatar_url, created_at, last_seen_at "
                        + "FROM app_user WHERE openid = ?",
                MAPPER, openid);
    }

    public AppUser updateProfile(String openid, String nickname, String avatarUrl) {
        touch(openid);
        jdbc.update("UPDATE app_user SET nickname = ?, avatar_url = ? WHERE openid = ?",
                nickname, avatarUrl, openid);
        return jdbc.queryForObject(
                "SELECT id, openid, nickname, avatar_url, created_at, last_seen_at "
                        + "FROM app_user WHERE openid = ?",
                MAPPER, openid);
    }
}
