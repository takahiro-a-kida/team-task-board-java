package com.example.taskboard.team;

import java.time.Instant;

public record Team(long id, String name, Instant createdAt) {
}
