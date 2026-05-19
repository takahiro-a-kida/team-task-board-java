package com.example.taskboard.task;

import java.time.LocalDate;
import java.util.List;
import com.example.taskboard.TestFixtures;
import com.example.taskboard.task.dto.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired TaskRepository taskRepository;
    @Autowired TestFixtures fixtures;

    private long teamA;
    private long teamB;
    private long alice;
    private long bob;

    @BeforeEach
    void setUp() {
        fixtures.resetDatabase();
        alice = fixtures.insertUser("alice", "アリス");
        bob = fixtures.insertUser("bob", "ボブ");
        teamA = fixtures.insertTeam("チームA");
        teamB = fixtures.insertTeam("チームB");
        fixtures.addMember(teamA, alice, "OWNER");
        fixtures.addMember(teamA, bob, "MEMBER");
        fixtures.addMember(teamB, alice, "OWNER");
    }

    @Test
    @DisplayName("チーム外のタスクは検索結果に含まれない")
    void searchIsScopedByTeam() {
        long inA = fixtures.insertTask(teamA, "AのTODO", "", "TODO", "LOW", null, null, alice);
        fixtures.insertTask(teamB, "BのTODO", "", "TODO", "LOW", null, null, alice);

        List<Task> result = taskRepository.search(teamA, empty());

        assertThat(result).extracting(Task::id).containsExactly(inA);
    }

    @Test
    @DisplayName("ステータスでの絞り込みが効く")
    void searchByStatus() {
        long todo = fixtures.insertTask(teamA, "未着手", "", "TODO", "LOW", null, null, alice);
        long inProg = fixtures.insertTask(teamA, "作業中", "", "IN_PROGRESS", "LOW", null, null, alice);
        fixtures.insertTask(teamA, "完了", "", "DONE", "LOW", null, null, alice);

        var c = new SearchCriteria(null, List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS), null, null, null, null, null);

        List<Task> result = taskRepository.search(teamA, c);
        assertThat(result).extracting(Task::id).containsExactlyInAnyOrder(todo, inProg);
    }

    @Test
    @DisplayName("優先度・担当者・未割当・期限範囲の組合せ絞り込みが効く")
    void searchByCompositeFilters() {
        long t1 = fixtures.insertTask(teamA, "HIGH 自分", "", "TODO", "HIGH", alice, "2026-05-20", alice);
        fixtures.insertTask(teamA, "HIGH 他人", "", "TODO", "HIGH", bob, "2026-05-20", alice);
        fixtures.insertTask(teamA, "LOW 自分", "", "TODO", "LOW", alice, "2026-05-20", alice);
        long t4 = fixtures.insertTask(teamA, "HIGH 未割当", "", "TODO", "HIGH", null, "2026-05-25", alice);
        fixtures.insertTask(teamA, "HIGH 期限外", "", "TODO", "HIGH", alice, "2026-06-30", alice);

        var byAssignee = new SearchCriteria(null, null, List.of(TaskPriority.HIGH), alice, false, LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-31"));
        assertThat(taskRepository.search(teamA, byAssignee))
                .extracting(Task::id)
                .containsExactly(t1);

        var unassigned = new SearchCriteria(null, null, List.of(TaskPriority.HIGH), null, true, null, LocalDate.parse("2026-05-31"));
        assertThat(taskRepository.search(teamA, unassigned))
                .extracting(Task::id)
                .containsExactly(t4);
    }

    @Test
    @DisplayName("キーワード検索: タイトルに含まれる語にヒットする")
    void searchByKeyword_titleHit() {
        long hit = fixtures.insertTask(teamA, "BCryptパスワード対応", "", "TODO", "LOW", null, null, alice);
        fixtures.insertTask(teamA, "別の機能", "全く関係ない説明", "TODO", "LOW", null, null, alice);

        var c = new SearchCriteria("BCrypt", null, null, null, null, null, null);

        List<Task> result = taskRepository.search(teamA, c);
        assertThat(result).extracting(Task::id).containsExactly(hit);
    }

    // --- 演習で受講者に追加してもらうケース -----------------------------------
    // 受講者は UI 上で「タイトルにあるキーワードは出るのに、説明文にあるキーワードが
    // 出てこない」現象を踏み、ここに以下のようなテストを追加して RED にすることから
    // 始める想定。
    //
    // @Test
    // @DisplayName("キーワード検索: 説明文に含まれる語にもヒットする")
    // void searchByKeyword_descriptionHit() {
    //     long hit = fixtures.insertTask(teamA, "認証機能の整備", "BCryptハッシュで保存すること", "TODO", "LOW", null, null, alice);
    //     fixtures.insertTask(teamA, "全く関係ないタスク", "本文も無関係", "TODO", "LOW", null, null, alice);
    //
    //     var c = new SearchCriteria("BCrypt", null, null, null, null, null, null);
    //
    //     List<Task> result = taskRepository.search(teamA, c);
    //     assertThat(result).extracting(Task::id).containsExactly(hit);
    // }

    @Test
    @DisplayName("キーワードがどこにも無ければ空")
    void searchByKeyword_noHit() {
        fixtures.insertTask(teamA, "タイトル", "本文", "TODO", "LOW", null, null, alice);

        var c = new SearchCriteria("notfound", null, null, null, null, null, null);

        assertThat(taskRepository.search(teamA, c)).isEmpty();
    }

    @Test
    @DisplayName("優先度順 → 期限が近い順 → id 順に並ぶ")
    void searchOrdering() {
        long low = fixtures.insertTask(teamA, "LOW", "", "TODO", "LOW", null, "2026-05-10", alice);
        long highLate = fixtures.insertTask(teamA, "HIGHおそい", "", "TODO", "HIGH", null, "2026-06-30", alice);
        long highSoon = fixtures.insertTask(teamA, "HIGHはやい", "", "TODO", "HIGH", null, "2026-05-05", alice);
        long med = fixtures.insertTask(teamA, "MEDIUM", "", "TODO", "MEDIUM", null, null, alice);

        List<Task> result = taskRepository.search(teamA, empty());

        assertThat(result).extracting(Task::id).containsExactly(highSoon, highLate, med, low);
    }

    @Test
    @DisplayName("update / updateStatus / delete の往復が動く")
    void crudRoundTrip() {
        long id = taskRepository.insert(teamA, "オリジナル", "desc", TaskPriority.LOW, alice, LocalDate.parse("2026-06-01"), alice);

        Task original = taskRepository.findById(id).orElseThrow();
        assertThat(original.status()).isEqualTo(TaskStatus.TODO);
        assertThat(original.dueDate()).isEqualTo(LocalDate.parse("2026-06-01"));

        taskRepository.update(id, "変更後", "newdesc", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, bob, null);
        Task after = taskRepository.findById(id).orElseThrow();
        assertThat(after.title()).isEqualTo("変更後");
        assertThat(after.assigneeUserId()).isEqualTo(bob);
        assertThat(after.dueDate()).isNull();

        taskRepository.updateStatus(id, TaskStatus.DONE);
        assertThat(taskRepository.findById(id).orElseThrow().status()).isEqualTo(TaskStatus.DONE);

        taskRepository.delete(id);
        assertThat(taskRepository.findById(id)).isEmpty();
    }

    private static SearchCriteria empty() {
        return new SearchCriteria(null, null, null, null, null, null, null);
    }
}
