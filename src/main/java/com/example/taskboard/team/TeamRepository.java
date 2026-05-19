package com.example.taskboard.team;

import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import com.example.taskboard.user.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TeamRepository {

    private final JdbcClient jdbc;

    public TeamRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Team> TEAM_ROW = (ResultSet rs, int i) -> new Team(
            rs.getLong("id"),
            rs.getString("name"),
            UserRepository.parseInstant(rs, "created_at")
    );

    private static final RowMapper<TeamMember> MEMBER_ROW = (ResultSet rs, int i) -> new TeamMember(
            rs.getLong("team_id"),
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getString("display_name"),
            TeamRole.valueOf(rs.getString("role"))
    );

    public List<Team> findTeamsForUser(long userId) {
        return jdbc.sql("""
                SELECT t.id, t.name, t.created_at
                FROM teams t
                JOIN team_members tm ON tm.team_id = t.id
                WHERE tm.user_id = :uid
                ORDER BY t.name
                """)
                .param("uid", userId)
                .query(TEAM_ROW)
                .list();
    }

    public Optional<Team> findById(long teamId) {
        return jdbc.sql("SELECT id, name, created_at FROM teams WHERE id = :id")
                .param("id", teamId)
                .query(TEAM_ROW)
                .optional();
    }

    public long createTeam(String name, long ownerUserId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO teams(name) VALUES (:name)")
                .param("name", name)
                .update(keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("生成された team id が取得できませんでした");
        }
        long teamId = id.longValue();
        addMember(teamId, ownerUserId, TeamRole.OWNER);
        return teamId;
    }

    public void addMember(long teamId, long userId, TeamRole role) {
        try {
            jdbc.sql("INSERT INTO team_members(team_id, user_id, role) VALUES (:t,:u,:r)")
                    .param("t", teamId)
                    .param("u", userId)
                    .param("r", role.name())
                    .update();
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("既にチームに所属しています", e);
        }
    }

    public void updateRole(long teamId, long userId, TeamRole role) {
        jdbc.sql("UPDATE team_members SET role = :r WHERE team_id = :t AND user_id = :u")
                .param("r", role.name())
                .param("t", teamId)
                .param("u", userId)
                .update();
    }

    public void removeMember(long teamId, long userId) {
        jdbc.sql("DELETE FROM team_members WHERE team_id = :t AND user_id = :u")
                .param("t", teamId)
                .param("u", userId)
                .update();
    }

    public List<TeamMember> listMembers(long teamId) {
        return jdbc.sql("""
                SELECT tm.team_id, tm.user_id, tm.role, u.username, u.display_name
                FROM team_members tm
                JOIN users u ON u.id = tm.user_id
                WHERE tm.team_id = :t
                ORDER BY tm.role, u.username
                """)
                .param("t", teamId)
                .query(MEMBER_ROW)
                .list();
    }

    public Optional<TeamRole> findRole(long teamId, long userId) {
        return jdbc.sql("SELECT role FROM team_members WHERE team_id = :t AND user_id = :u")
                .param("t", teamId)
                .param("u", userId)
                .query((rs, i) -> TeamRole.valueOf(rs.getString("role")))
                .optional();
    }

    public int countOwners(long teamId) {
        return jdbc.sql("SELECT COUNT(*) FROM team_members WHERE team_id = :t AND role = 'OWNER'")
                .param("t", teamId)
                .query(Integer.class)
                .single();
    }
}
