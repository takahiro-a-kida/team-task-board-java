package com.example.taskboard.team;

import java.util.List;
import com.example.taskboard.common.BadRequestException;
import com.example.taskboard.common.ForbiddenException;
import com.example.taskboard.common.NotFoundException;
import com.example.taskboard.user.UserRepository;
import com.example.taskboard.user.UserSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    public List<Team> teamsFor(long userId) {
        return teamRepository.findTeamsForUser(userId);
    }

    public TeamRole requireMembership(long teamId, long userId) {
        return teamRepository.findRole(teamId, userId)
                .orElseThrow(() -> new ForbiddenException("このチームへのアクセス権がありません"));
    }

    public void requireOwner(long teamId, long userId) {
        TeamRole role = requireMembership(teamId, userId);
        if (role != TeamRole.OWNER) {
            throw new ForbiddenException("この操作はチームの OWNER のみ実行できます");
        }
    }

    @Transactional
    public long createTeam(String name, long ownerUserId) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("チーム名は必須です");
        }
        return teamRepository.createTeam(name.trim(), ownerUserId);
    }

    public List<TeamMember> listMembers(long teamId, long requesterUserId) {
        requireMembership(teamId, requesterUserId);
        return teamRepository.listMembers(teamId);
    }

    @Transactional
    public void addMember(long teamId, String username, TeamRole role, long requesterUserId) {
        requireOwner(teamId, requesterUserId);
        UserSummary user = userRepository.findByUsername(username)
                .map(u -> new UserSummary(u.id(), u.username(), u.displayName()))
                .orElseThrow(() -> new NotFoundException("ユーザーが存在しません: " + username));
        teamRepository.addMember(teamId, user.id(), role == null ? TeamRole.MEMBER : role);
    }

    @Transactional
    public void changeRole(long teamId, long targetUserId, TeamRole newRole, long requesterUserId) {
        requireOwner(teamId, requesterUserId);
        TeamRole current = teamRepository.findRole(teamId, targetUserId)
                .orElseThrow(() -> new NotFoundException("対象ユーザーはチームに所属していません"));
        if (current == newRole) {
            return;
        }
        if (current == TeamRole.OWNER && newRole == TeamRole.MEMBER && teamRepository.countOwners(teamId) <= 1) {
            throw new BadRequestException("最後の OWNER は降格できません");
        }
        teamRepository.updateRole(teamId, targetUserId, newRole);
    }

    @Transactional
    public void removeMember(long teamId, long targetUserId, long requesterUserId) {
        requireOwner(teamId, requesterUserId);
        TeamRole current = teamRepository.findRole(teamId, targetUserId)
                .orElseThrow(() -> new NotFoundException("対象ユーザーはチームに所属していません"));
        if (current == TeamRole.OWNER && teamRepository.countOwners(teamId) <= 1) {
            throw new BadRequestException("最後の OWNER は削除できません。先に他のメンバーを OWNER に昇格してください");
        }
        teamRepository.removeMember(teamId, targetUserId);
    }
}
