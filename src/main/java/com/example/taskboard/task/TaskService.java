package com.example.taskboard.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.taskboard.common.NotFoundException;
import com.example.taskboard.task.dto.CreateTaskRequest;
import com.example.taskboard.task.dto.SearchCriteria;
import com.example.taskboard.task.dto.TaskResponse;
import com.example.taskboard.task.dto.UpdateTaskRequest;
import com.example.taskboard.team.TeamService;
import com.example.taskboard.user.UserRepository;
import com.example.taskboard.user.UserSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamService teamService;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TeamService teamService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.teamService = teamService;
    }

    public List<TaskResponse> search(long teamId, long requesterUserId, SearchCriteria criteria) {
        teamService.requireMembership(teamId, requesterUserId);
        List<Task> tasks = taskRepository.search(teamId, criteria);
        return enrich(tasks);
    }

    public TaskResponse find(long teamId, long taskId, long requesterUserId) {
        teamService.requireMembership(teamId, requesterUserId);
        Task t = loadAndValidate(teamId, taskId);
        return enrich(List.of(t)).get(0);
    }

    @Transactional
    public long create(long teamId, long requesterUserId, CreateTaskRequest req) {
        teamService.requireMembership(teamId, requesterUserId);
        if (req.assigneeUserId() != null) {
            teamService.requireMembership(teamId, req.assigneeUserId());
        }
        return taskRepository.insert(
                teamId, req.title().trim(), req.description(), req.priority(),
                req.assigneeUserId(), req.dueDate(), requesterUserId);
    }

    @Transactional
    public void update(long teamId, long taskId, long requesterUserId, UpdateTaskRequest req) {
        teamService.requireMembership(teamId, requesterUserId);
        loadAndValidate(teamId, taskId);
        if (req.assigneeUserId() != null) {
            teamService.requireMembership(teamId, req.assigneeUserId());
        }
        taskRepository.update(taskId, req.title().trim(), req.description(), req.status(),
                req.priority(), req.assigneeUserId(), req.dueDate());
    }

    @Transactional
    public void changeStatus(long teamId, long taskId, long requesterUserId, TaskStatus status) {
        teamService.requireMembership(teamId, requesterUserId);
        loadAndValidate(teamId, taskId);
        taskRepository.updateStatus(taskId, status);
    }

    @Transactional
    public void delete(long teamId, long taskId, long requesterUserId) {
        teamService.requireMembership(teamId, requesterUserId);
        loadAndValidate(teamId, taskId);
        taskRepository.delete(taskId);
    }

    private Task loadAndValidate(long teamId, long taskId) {
        Task t = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("タスクが見つかりません: " + taskId));
        if (t.teamId() != teamId) {
            throw new NotFoundException("このチームのタスクではありません: " + taskId);
        }
        return t;
    }

    private List<TaskResponse> enrich(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        List<Long> ids = taskRepository.collectAssigneeIds(tasks);
        Map<Long, UserSummary> users = new HashMap<>();
        for (UserSummary s : userRepository.findSummariesByIds(ids)) {
            users.put(s.id(), s);
        }
        List<TaskResponse> out = new ArrayList<>(tasks.size());
        for (Task t : tasks) {
            UserSummary assignee = t.assigneeUserId() == null ? null : users.get(t.assigneeUserId());
            UserSummary createdBy = users.get(t.createdByUserId());
            out.add(TaskResponse.of(t, assignee, createdBy));
        }
        return out;
    }
}
