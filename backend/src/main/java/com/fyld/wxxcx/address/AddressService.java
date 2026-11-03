package com.fyld.wxxcx.address;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Service
public class AddressService {

    /** 同一门牌只允许一个业主登记 */
    public static final String DUP_ADDRESS_MESSAGE = "您输入的业主信息已被注册，如有疑问请联系管理员";

    private static final String SELECT_SQL =
            "SELECT id, province, city, community, building, unit, room_no, created_at FROM user_address";

    private static final RowMapper<UserAddress> MAPPER = (rs, rowNum) -> new UserAddress(
            rs.getLong("id"),
            rs.getString("province"),
            rs.getString("city"),
            rs.getString("community"),
            rs.getString("building"),
            rs.getString("unit"),
            rs.getString("room_no"),
            rs.getTimestamp("created_at").toLocalDateTime());

    private final JdbcTemplate jdbc;

    public AddressService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 未登记返回 null；一个 openid 至多一行，由 uk_openid 保证 */
    public UserAddress findOne(String openid) {
        List<UserAddress> rows = jdbc.query(SELECT_SQL + " WHERE openid = ?", MAPPER, openid);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public UserAddress create(String openid, AddressInput input) {
        Integer dup = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_address "
                        + "WHERE province = ? AND city = ? AND community = ? AND building = ? AND unit = ? AND room_no = ?",
                Integer.class,
                input.province(), input.city(), input.community(),
                input.building(), input.unit(), input.roomNo());
        if (dup != null && dup > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DUP_ADDRESS_MESSAGE);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO user_address (openid, province, city, community, building, unit, room_no) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, openid);
            ps.setString(2, input.province());
            ps.setString(3, input.city());
            ps.setString(4, input.community());
            ps.setString(5, input.building());
            ps.setString(6, input.unit());
            ps.setString(7, input.roomNo());
            return ps;
        }, keyHolder);

        long id = keyHolder.getKey().longValue();
        return jdbc.queryForObject(SELECT_SQL + " WHERE id = ?", MAPPER, id);
    }
}
