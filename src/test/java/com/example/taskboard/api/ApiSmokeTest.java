package com.example.taskboard.api;

import com.example.taskboard.TestFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSmokeTest {

    @Autowired MockMvc mvc;
    @Autowired TestFixtures fixtures;
    @Autowired ObjectMapper om;

    private long alice;
    private long bob;
    private long teamA;

    @BeforeEach
    void setUp() {
        fixtures.resetDatabase();
        alice = fixtures.insertUser("alice", "アリス");
        bob = fixtures.insertUser("bob", "ボブ");
        teamA = fixtures.insertTeam("製品開発チーム");
        fixtures.addMember(teamA, alice, "OWNER");
        fixtures.addMember(teamA, bob, "MEMBER");
    }

    @Test
    @DisplayName("未ログインで /api/me は 401")
    void anonymousRejected() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ログイン済みなら自分の情報・チーム・タスク作成・状態変更が一通り動く")
    @WithUserDetails(value = "alice", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void loggedInHappyPath() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));

        mvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("製品開発チーム"));

        String body = """
                {"title":"スモーク","description":"作る","priority":"MEDIUM","assigneeUserId":%d}
                """.formatted(bob);
        MvcResult create = mvc.perform(post("/api/teams/{teamId}/tasks", teamA)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        long taskId = Long.parseLong(create.getResponse().getContentAsString());

        mvc.perform(get("/api/teams/{teamId}/tasks/{taskId}", teamA, taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("スモーク"))
                .andExpect(jsonPath("$.assignee.username").value("bob"));

        mvc.perform(patch("/api/teams/{teamId}/tasks/{taskId}/status", teamA, taskId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNoContent());

        MvcResult list = mvc.perform(get("/api/teams/{teamId}/tasks", teamA)
                        .param("status", "DONE"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode arr = om.readTree(list.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(1);
        assertThat(arr.get(0).get("id").asLong()).isEqualTo(taskId);
    }

    @Test
    @DisplayName("所属外チームのタスクは 403")
    @WithUserDetails(value = "alice", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void foreignTeamForbidden() throws Exception {
        long other = fixtures.insertTeam("無関係チーム");
        long charlie = fixtures.insertUser("charlie", "チャーリー");
        fixtures.addMember(other, charlie, "OWNER");

        mvc.perform(get("/api/teams/{teamId}/tasks", other))
                .andExpect(status().isForbidden());
    }
}
