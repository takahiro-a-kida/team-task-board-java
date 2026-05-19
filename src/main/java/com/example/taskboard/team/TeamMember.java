package com.example.taskboard.team;

public record TeamMember(long teamId, long userId, String username, String displayName, TeamRole role) {
}
