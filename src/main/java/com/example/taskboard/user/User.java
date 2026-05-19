package com.example.taskboard.user;

import java.time.Instant;

public record User(
        long id,
        String username,
        String passwordHash,
        String displayName,
        Instant createdAt
) {
}
