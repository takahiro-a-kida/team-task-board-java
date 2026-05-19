package com.example.taskboard.task;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.example.taskboard.task.dto.SearchCriteria;
import com.example.taskboard.user.UserRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {

    private final JdbcClient jdbc;

    public TaskRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Task> ROW = (ResultSet rs, int i) -> {
        Long assignee = rs.getObject("assignee_user_id") == null ? null : rs.getLong("assignee_user_id");
        String due = rs.getString("due_date");
        return new Task(
                rs.getLong("id"),
                rs.getLong("team_id"),
                rs.getString("title"),
                rs.getString("description"),
                TaskStatus.valueOf(rs.getString("status")),
                TaskPriority.valueOf(rs.getString("priority")),
                assignee,
                due == null ? null : LocalDate.parse(due),
                rs.getLong("created_by_user_id"),
                UserRepository.parseInstant(rs, "created_at"),
                UserRepository.parseInstant(rs, "updated_at")
        );
    };

    public Optional<Task> findById(long id) {
        return jdbc.sql("SELECT * FROM tasks WHERE id = :id")
                .param("id", id)
                .query(ROW)
                .optional();
    }

    public long insert(long teamId, String title, String description, TaskPriority priority,
                       Long assigneeUserId, LocalDate dueDate, long createdByUserId) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO tasks(team_id, title, description, status, priority,
                                  assignee_user_id, due_date, created_by_user_id)
                VALUES (:teamId, :title, :description, 'TODO', :priority,
                        :assignee, :dueDate, :createdBy)
                """)
                .param("teamId", teamId)
                .param("title", title)
                .param("description", description == null ? "" : description)
                .param("priority", priority.name())
                .param("assignee", assigneeUserId)
                .param("dueDate", dueDate == null ? null : dueDate.toString())
                .param("createdBy", createdByUserId)
                .update(kh);
        Number id = kh.getKey();
        if (id == null) {
            throw new IllegalStateException("生成された task id が取得できませんでした");
        }
        return id.longValue();
    }

    public void update(long id, String title, String description, TaskStatus status, TaskPriority priority,
                       Long assigneeUserId, LocalDate dueDate) {
        jdbc.sql("""
                UPDATE tasks
                SET title = :title,
                    description = :description,
                    status = :status,
                    priority = :priority,
                    assignee_user_id = :assignee,
                    due_date = :dueDate,
                    updated_at = :now
                WHERE id = :id
                """)
                .param("title", title)
                .param("description", description == null ? "" : description)
                .param("status", status.name())
                .param("priority", priority.name())
                .param("assignee", assigneeUserId)
                .param("dueDate", dueDate == null ? null : dueDate.toString())
                .param("now", Instant.now().toString())
                .param("id", id)
                .update();
    }

    public void updateStatus(long id, TaskStatus status) {
        jdbc.sql("UPDATE tasks SET status = :status, updated_at = :now WHERE id = :id")
                .param("status", status.name())
                .param("now", Instant.now().toString())
                .param("id", id)
                .update();
    }

    public void delete(long id) {
        jdbc.sql("DELETE FROM tasks WHERE id = :id").param("id", id).update();
    }

    public List<Task> search(long teamId, SearchCriteria criteria) {
        StringBuilder sql = new StringBuilder("SELECT * FROM tasks WHERE team_id = :teamId");
        Map<String, Object> params = new HashMap<>();
        params.put("teamId", teamId);

        if (criteria.hasKeyword()) {
            String kw = criteria.keyword().trim().toLowerCase();
            sql.append(" AND (LOWER(title) LIKE :kwTitle OR LOWER(description) LIKE :kwDesc)");
            params.put("kwTitle", "%" + kw + "%");
            params.put("kwDesc", kw);
        }

        if (criteria.statuses() != null && !criteria.statuses().isEmpty()) {
            sql.append(" AND status IN (:statuses)");
            params.put("statuses", criteria.statuses().stream().map(Enum::name).toList());
        }

        if (criteria.priorities() != null && !criteria.priorities().isEmpty()) {
            sql.append(" AND priority IN (:priorities)");
            params.put("priorities", criteria.priorities().stream().map(Enum::name).toList());
        }

        if (Boolean.TRUE.equals(criteria.assigneeUnassigned())) {
            sql.append(" AND assignee_user_id IS NULL");
        } else if (criteria.assigneeUserId() != null) {
            sql.append(" AND assignee_user_id = :assignee");
            params.put("assignee", criteria.assigneeUserId());
        }

        if (criteria.dueFrom() != null) {
            sql.append(" AND due_date IS NOT NULL AND due_date >= :dueFrom");
            params.put("dueFrom", criteria.dueFrom().toString());
        }

        if (criteria.dueTo() != null) {
            sql.append(" AND due_date IS NOT NULL AND due_date <= :dueTo");
            params.put("dueTo", criteria.dueTo().toString());
        }

        sql.append(" ORDER BY ")
           .append("CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END, ")
           .append("CASE WHEN due_date IS NULL THEN 1 ELSE 0 END, due_date ASC, id ASC");

        var spec = jdbc.sql(sql.toString());
        for (Map.Entry<String, Object> e : params.entrySet()) {
            spec = spec.param(e.getKey(), e.getValue());
        }
        return spec.query(ROW).list();
    }

    public List<Long> collectAssigneeIds(List<Task> tasks) {
        var ids = new ArrayList<Long>();
        for (Task t : tasks) {
            if (t.assigneeUserId() != null && !ids.contains(t.assigneeUserId())) {
                ids.add(t.assigneeUserId());
            }
            if (!ids.contains(t.createdByUserId())) {
                ids.add(t.createdByUserId());
            }
        }
        return ids;
    }
}
