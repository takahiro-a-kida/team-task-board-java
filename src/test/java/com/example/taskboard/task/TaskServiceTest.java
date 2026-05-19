package com.example.taskboard.task;

import com.example.taskboard.TestFixtures;
import com.example.taskboard.common.ForbiddenException;
import com.example.taskboard.common.NotFoundException;
import com.example.taskboard.task.dto.CreateTaskRequest;
import com.example.taskboard.task.dto.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TaskServiceTest {

    @Autowired TaskService taskService;
    @Autowired TestFixtures fixtures;

    private long teamA;
    private long teamB;
    private long alice;
    private long bob;
    private long stranger;

    @BeforeEach
    void setUp() {
        fixtures.resetDatabase();
        alice = fixtures.insertUser("alice", "アリス");
        bob = fixtures.insertUser("bob", "ボブ");
        stranger = fixtures.insertUser("stranger", "他人");
        teamA = fixtures.insertTeam("チームA");
        teamB = fixtures.insertTeam("チームB");
        fixtures.addMember(teamA, alice, "OWNER");
        fixtures.addMember(teamA, bob, "MEMBER");
        fixtures.addMember(teamB, alice, "OWNER");
    }

    @Test
    @DisplayName("所属外のチームのタスク検索は 403")
    void cannotSearchForeignTeam() {
        assertThatThrownBy(() -> taskService.search(teamA, stranger, empty()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("チーム不一致の taskId 指定は 404")
    void cannotAccessWrongTeamTask() {
        long inB = fixtures.insertTask(teamB, "Bのタスク", "", "TODO", "LOW", null, null, alice);
        assertThatThrownBy(() -> taskService.find(teamA, inB, alice))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("チームメンバーでないユーザーを担当者に設定すると 403")
    void cannotAssignNonMember() {
        var req = new CreateTaskRequest("テスト", "本文", TaskPriority.LOW, stranger, null);
        assertThatThrownBy(() -> taskService.create(teamA, alice, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("正常作成と find で同じ内容を返す")
    void createAndFind() {
        var req = new CreateTaskRequest("作って", "ね", TaskPriority.HIGH, bob, null);
        long id = taskService.create(teamA, alice, req);

        var resp = taskService.find(teamA, id, alice);
        assertThat(resp.title()).isEqualTo("作って");
        assertThat(resp.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(resp.assignee().username()).isEqualTo("bob");
        assertThat(resp.createdBy().username()).isEqualTo("alice");
    }

    private static SearchCriteria empty() {
        return new SearchCriteria(null, null, null, null, null, null, null);
    }
}
