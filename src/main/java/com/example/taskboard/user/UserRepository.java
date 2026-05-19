package com.example.taskboard.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<User> USER_ROW = (ResultSet rs, int i) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("display_name"),
            parseInstant(rs, "created_at")
    );

    private static final RowMapper<UserSummary> SUMMARY_ROW = (ResultSet rs, int i) -> new UserSummary(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("display_name")
    );

    public static Instant parseInstant(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        if (raw == null) {
            return null;
        }
        try {
            Timestamp ts = rs.getTimestamp(column);
            if (ts != null) {
                return ts.toInstant();
            }
        } catch (SQLException ignore) {
            // fallback to manual parse below
        }
        return Instant.parse(raw.replace(' ', 'T') + (raw.contains("Z") ? "" : "Z"));
    }

    public Optional<User> findByUsername(String username) {
        return jdbc.sql("SELECT * FROM users WHERE username = :u")
                .param("u", username)
                .query(USER_ROW)
                .optional();
    }

    public Optional<UserSummary> findSummaryById(long id) {
        return jdbc.sql("SELECT id, username, display_name FROM users WHERE id = :id")
                .param("id", id)
                .query(SUMMARY_ROW)
                .optional();
    }

    public List<UserSummary> searchByUsername(String q, int limit) {
        String pattern = "%" + q.toLowerCase() + "%";
        return jdbc.sql("""
                SELECT id, username, display_name
                FROM users
                WHERE LOWER(username) LIKE :q OR LOWER(display_name) LIKE :q
                ORDER BY username
                LIMIT :limit
                """)
                .param("q", pattern)
                .param("limit", limit)
                .query(SUMMARY_ROW)
                .list();
    }

    public List<UserSummary> findSummariesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT id, username, display_name FROM users WHERE id IN (:ids)")
                .param("ids", ids)
                .query(SUMMARY_ROW)
                .list();
    }
}
