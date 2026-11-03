package com.fyld.wxxcx.vote;

import com.fyld.wxxcx.address.AddressService;
import com.fyld.wxxcx.address.UserAddress;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VoteService {

    public static final String SUPPORT = "SUPPORT";
    public static final String OPPOSE = "OPPOSE";

    private static final RowMapper<VoteItem> ITEM_MAPPER = (rs, rowNum) -> new VoteItem(
            rs.getString("address"),
            rs.getString("choice"),
            rs.getTimestamp("voted_at").toLocalDateTime());

    private final JdbcTemplate jdbc;
    private final AddressService addressService;

    public VoteService(JdbcTemplate jdbc, AddressService addressService) {
        this.jdbc = jdbc;
        this.addressService = addressService;
    }

    public VoteResult vote(String openid, String title, String choice) {
        UserAddress me = requireAddress(openid);
        long topicId = topicIdOf(title, openid, me);
        saveRecord(topicId, openid, choice, me.line());
        return new VoteResult(topicId, title, choice, me.line());
    }

    /** 对已存在的主题投票，不需要再传主题名 */
    public VoteResult voteOnTopic(String openid, long topicId, String choice) {
        UserAddress me = requireAddress(openid);
        List<String> titles = jdbc.query(
                "SELECT title FROM vote_topic "
                        + "WHERE id = ? AND province = ? AND city = ? AND community = ?",
                (rs, rowNum) -> rs.getString(1), topicId, me.province(), me.city(), me.community());
        if (titles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "投票主题不存在");
        }
        saveRecord(topicId, openid, choice, me.line());
        return new VoteResult(topicId, titles.get(0), choice, me.line());
    }

    private UserAddress requireAddress(String openid) {
        UserAddress address = addressService.findOne(openid);
        if (address == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "请先登记楼栋信息");
        }
        return address;
    }

    private void saveRecord(long topicId, String openid, String choice, String line) {
        int updated = jdbc.update(
                "UPDATE vote_record SET choice = ?, address = ?, voted_at = CURRENT_TIMESTAMP "
                        + "WHERE topic_id = ? AND openid = ?",
                choice, line, topicId, openid);
        if (updated == 0) {
            jdbc.update("INSERT INTO vote_record (topic_id, openid, choice, address) VALUES (?, ?, ?, ?)",
                    topicId, openid, choice, line);
        }
    }

    /** 只展示发起人与当前用户同省、同市、同小区的主题 */
    public List<VoteTopic> listTopics(String openid) {
        UserAddress me = requireAddress(openid);
        return jdbc.query(
                "SELECT t.id, t.title, t.created_at, "
                        + "COALESCE(SUM(v.choice = 'SUPPORT'), 0) AS support_count, "
                        + "COALESCE(SUM(v.choice = 'OPPOSE'), 0) AS oppose_count, "
                        + "MAX(CASE WHEN v.openid = ? THEN v.choice END) AS my_choice "
                        + "FROM vote_topic t "
                        + "LEFT JOIN vote_record v ON v.topic_id = t.id "
                        + "WHERE t.province = ? AND t.city = ? AND t.community = ? "
                        + "GROUP BY t.id, t.title, t.created_at "
                        + "ORDER BY t.id DESC",
                (rs, rowNum) -> new VoteTopic(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getLong("support_count"),
                        rs.getLong("oppose_count"),
                        rs.getString("my_choice"),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                openid, me.province(), me.city(), me.community());
    }

    public TopicDetail getTopic(long id, String openid) {
        UserAddress me = requireAddress(openid);
        List<VoteTopic> topics = jdbc.query(
                "SELECT id, title, created_at FROM vote_topic "
                        + "WHERE id = ? AND province = ? AND city = ? AND community = ?",
                (rs, rowNum) -> new VoteTopic(
                        rs.getLong("id"),
                        rs.getString("title"),
                        0,
                        0,
                        null,
                        rs.getTimestamp("created_at").toLocalDateTime()),
                id, me.province(), me.city(), me.community());
        if (topics.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "投票主题不存在");
        }

        List<VoteItem> votes = jdbc.query(
                "SELECT address, choice, voted_at FROM vote_record WHERE topic_id = ? ORDER BY id",
                ITEM_MAPPER, id);
        List<String> mine = jdbc.query(
                "SELECT choice FROM vote_record WHERE topic_id = ? AND openid = ?",
                (rs, rowNum) -> rs.getString(1), id, openid);

        long support = votes.stream().filter(v -> SUPPORT.equals(v.choice())).count();
        VoteTopic topic = topics.get(0);
        return new TopicDetail(
                topic.id(),
                topic.title(),
                support,
                votes.size() - support,
                mine.isEmpty() ? null : mine.get(0),
                topic.createdAt(),
                votes,
                buildingSummary(votes));
    }

    /** 按栋+单元聚合，行内序号排：2 栋排在 10 栋前 */
    private List<BuildingSummary> buildingSummary(List<VoteItem> votes) {
        record Key(String building, String unit) {
        }
        Map<Key, long[]> counter = new LinkedHashMap<>();
        for (VoteItem vote : votes) {
            String[] parts = vote.address().split("-", 3);
            if (parts.length < 3) {
                continue;
            }
            long[] counts = counter.computeIfAbsent(new Key(parts[0], parts[1]), k -> new long[2]);
            counts[OPPOSE.equals(vote.choice()) ? 1 : 0]++;
        }
        return counter.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<Key, long[]> e) -> Long.parseLong(e.getKey().building()))
                        .thenComparing(e -> Long.parseLong(e.getKey().unit())))
                .map(e -> new BuildingSummary(
                        e.getKey().building(),
                        e.getKey().unit(),
                        e.getValue()[0],
                        e.getValue()[1]))
                .toList();
    }

    /** 标题在小区内唯一：同小区已存在就复用；不同小区可以同名，互不影响 */
    private long topicIdOf(String title, String openid, UserAddress me) {
        List<Long> ids = jdbc.query(
                "SELECT id FROM vote_topic "
                        + "WHERE title = ? AND province = ? AND city = ? AND community = ?",
                (rs, rowNum) -> rs.getLong(1), title, me.province(), me.city(), me.community());
        if (!ids.isEmpty()) {
            return ids.get(0);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO vote_topic (title, openid, province, city, community) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, title);
                ps.setString(2, openid);
                ps.setString(3, me.province());
                ps.setString(4, me.city());
                ps.setString(5, me.community());
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException e) {
            // 并发下同小区撞 uk_community_title，复查拿已有 id
            return jdbc.queryForObject(
                    "SELECT id FROM vote_topic "
                            + "WHERE title = ? AND province = ? AND city = ? AND community = ?",
                    Long.class, title, me.province(), me.city(), me.community());
        }
        return keyHolder.getKey().longValue();
    }
}
