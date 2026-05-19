package com.example.taskboard.team;

import com.example.taskboard.TestFixtures;
import com.example.taskboard.common.BadRequestException;
import com.example.taskboard.common.ForbiddenException;
import com.example.taskboard.common.NotFoundException;
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
class TeamServiceTest {

    @Autowired TeamService teamService;
    @Autowired TeamRepository teamRepository;
    @Autowired TestFixtures fixtures;

    private long owner;
    private long member;
    private long stranger;
    private long teamId;

    @BeforeEach
    void setUp() {
        fixtures.resetDatabase();
        owner = fixtures.insertUser("owner", "オーナー");
        member = fixtures.insertUser("member", "メンバー");
        stranger = fixtures.insertUser("stranger", "他人");
        teamId = fixtures.insertTeam("テストチーム");
        fixtures.addMember(teamId, owner, "OWNER");
        fixtures.addMember(teamId, member, "MEMBER");
    }

    @Test
    @DisplayName("非メンバーは閲覧で 403")
    void nonMemberCannotList() {
        assertThatThrownBy(() -> teamService.listMembers(teamId, stranger))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("MEMBER はメンバー追加できない")
    void memberCannotAddMember() {
        assertThatThrownBy(() -> teamService.addMember(teamId, "stranger", TeamRole.MEMBER, member))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("OWNER はメンバー追加できる")
    void ownerCanAddMember() {
        teamService.addMember(teamId, "stranger", TeamRole.MEMBER, owner);
        assertThat(teamRepository.findRole(teamId, stranger)).contains(TeamRole.MEMBER);
    }

    @Test
    @DisplayName("最後の OWNER は降格できない")
    void cannotDemoteLastOwner() {
        assertThatThrownBy(() -> teamService.changeRole(teamId, owner, TeamRole.MEMBER, owner))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("最後の OWNER");
    }

    @Test
    @DisplayName("もう1人 OWNER を作れば降格できる")
    void canDemoteWhenAnotherOwnerExists() {
        teamService.changeRole(teamId, member, TeamRole.OWNER, owner);
        teamService.changeRole(teamId, owner, TeamRole.MEMBER, member);
        assertThat(teamRepository.findRole(teamId, owner)).contains(TeamRole.MEMBER);
    }

    @Test
    @DisplayName("最後の OWNER は削除できない")
    void cannotRemoveLastOwner() {
        assertThatThrownBy(() -> teamService.removeMember(teamId, owner, owner))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("存在しないユーザーを追加すると 404")
    void addNonexistentUser() {
        assertThatThrownBy(() -> teamService.addMember(teamId, "ghost", TeamRole.MEMBER, owner))
                .isInstanceOf(NotFoundException.class);
    }
}
