package com.example.taskboard.task;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import com.example.taskboard.task.dto.ChangeStatusRequest;
import com.example.taskboard.task.dto.CreateTaskRequest;
import com.example.taskboard.task.dto.SearchCriteria;
import com.example.taskboard.task.dto.TaskResponse;
import com.example.taskboard.task.dto.UpdateTaskRequest;
import com.example.taskboard.user.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams/{teamId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> list(@PathVariable long teamId,
                                   @RequestParam(value = "keyword", required = false) String keyword,
                                   @RequestParam(value = "status", required = false) String status,
                                   @RequestParam(value = "priority", required = false) String priority,
                                   @RequestParam(value = "assignee", required = false) String assignee,
                                   @RequestParam(value = "dueFrom", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
                                   @RequestParam(value = "dueTo", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo) {
        long me = CurrentUser.id();
        Long assigneeId = null;
        Boolean unassigned = null;
        if (assignee != null && !assignee.isBlank()) {
            if ("me".equalsIgnoreCase(assignee)) {
                assigneeId = me;
            } else if ("none".equalsIgnoreCase(assignee)) {
                unassigned = true;
            } else {
                try {
                    assigneeId = Long.parseLong(assignee);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("assignee は数値 / 'me' / 'none' のいずれかを指定してください");
                }
            }
        }
        List<TaskStatus> statuses = parseEnumList(status, TaskStatus.class);
        List<TaskPriority> priorities = parseEnumList(priority, TaskPriority.class);
        SearchCriteria criteria = new SearchCriteria(keyword, statuses, priorities, assigneeId, unassigned, dueFrom, dueTo);
        return taskService.search(teamId, me, criteria);
    }

    @GetMapping("/{taskId}")
    public TaskResponse get(@PathVariable long teamId, @PathVariable long taskId) {
        return taskService.find(teamId, taskId, CurrentUser.id());
    }

    @PostMapping
    public ResponseEntity<Long> create(@PathVariable long teamId, @Valid @RequestBody CreateTaskRequest req) {
        long id = taskService.create(teamId, CurrentUser.id(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<Void> update(@PathVariable long teamId, @PathVariable long taskId,
                                       @Valid @RequestBody UpdateTaskRequest req) {
        taskService.update(teamId, taskId, CurrentUser.id(), req);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable long teamId, @PathVariable long taskId,
                                             @Valid @RequestBody ChangeStatusRequest req) {
        taskService.changeStatus(teamId, taskId, CurrentUser.id(), req.status());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable long teamId, @PathVariable long taskId) {
        taskService.delete(teamId, taskId, CurrentUser.id());
        return ResponseEntity.noContent().build();
    }

    private static <E extends Enum<E>> List<E> parseEnumList(String raw, Class<E> type) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> Enum.valueOf(type, s.toUpperCase()))
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(type.getSimpleName() + " の値が不正です: " + raw);
        }
    }
}
