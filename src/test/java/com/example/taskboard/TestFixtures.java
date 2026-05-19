package com.example.taskboard;

import com.example.taskboard.user.UserSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class TestFixtures {

    @Autowired
    private JdbcClient jdbc;

    public void resetDatabase() {
        jdbc.sql("DELETE FROM tasks").update();
        jdbc.sql("DELETE FROM team_members").update();
        jdbc.sql("DELETE FROM teams").update();
        jdbc.sql("DELETE FROM users").update();
        jdbc.sql("DELETE FROM sqlite_sequence WHERE name IN ('tasks','teams','users')").update();
    }

    public long insertUser(String username, String displayName) {
        jdbc.sql("INSERT INTO users(username, password_hash, display_name) VALUES (:u, :p, :d)")
                .param("u", username)
                .param("p", "$2a$10$zPQXYgz2lbHHMJ49Het12.EYV49HD3XTA6Vg7S1HLJW3GOIUZoCT.")
                .param("d", displayName)
                .update();
        return jdbc.sql("SELECT id FROM users WHERE username = :u").param("u", username)
                .query(Long.class).single();
    }

    public long insertTeam(String name) {
        jdbc.sql("INSERT INTO teams(name) VALUES (:n)").param("n", name).update();
        return jdbc.sql("SELECT id FROM teams WHERE name = :n").param("n", name)
                .query(Long.class).single();
    }

    public void addMember(long teamId, long userId, String role) {
        jdbc.sql("INSERT INTO team_members(team_id, user_id, role) VALUES (:t,:u,:r)")
                .param("t", teamId).param("u", userId).param("r", role).update();
    }

    public long insertTask(long teamId, String title, String description, String status,
                           String priority, Long assignee, String dueDate, long createdBy) {
        jdbc.sql("""
                INSERT INTO tasks(team_id, title, description, status, priority,
                                  assignee_user_id, due_date, created_by_user_id)
                VALUES (:t,:title,:desc,:s,:p,:a,:d,:c)
                """)
                .param("t", teamId)
                .param("title", title)
                .param("desc", description)
                .param("s", status)
                .param("p", priority)
                .param("a", assignee)
                .param("d", dueDate)
                .param("c", createdBy)
                .update();
        return jdbc.sql("SELECT last_insert_rowid()").query(Long.class).single();
    }

    public UserSummary summary(long id) {
        return jdbc.sql("SELECT id, username, display_name FROM users WHERE id = :id")
                .param("id", id)
                .query((rs, i) -> new UserSummary(rs.getLong("id"), rs.getString("username"), rs.getString("display_name")))
                .single();
    }
}
