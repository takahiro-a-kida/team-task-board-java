package com.example.taskboard.team;

import java.util.List;
import com.example.taskboard.user.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public List<Team> myTeams() {
        return teamService.teamsFor(CurrentUser.id());
    }

    @PostMapping
    public ResponseEntity<Long> create(@Valid @RequestBody CreateTeamRequest req) {
        long id = teamService.createTeam(req.name(), CurrentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @GetMapping("/{teamId}/members")
    public List<TeamMember> members(@PathVariable long teamId) {
        return teamService.listMembers(teamId, CurrentUser.id());
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<Void> addMember(@PathVariable long teamId, @Valid @RequestBody AddMemberRequest req) {
        teamService.addMember(teamId, req.username(), req.role(), CurrentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> changeRole(@PathVariable long teamId,
                                           @PathVariable long userId,
                                           @Valid @RequestBody ChangeRoleRequest req) {
        teamService.changeRole(teamId, userId, req.role(), CurrentUser.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable long teamId, @PathVariable long userId) {
        teamService.removeMember(teamId, userId, CurrentUser.id());
        return ResponseEntity.noContent().build();
    }

    public record CreateTeamRequest(@NotBlank String name) {}
    public record AddMemberRequest(@NotBlank String username, TeamRole role) {}
    public record ChangeRoleRequest(TeamRole role) {}
}
