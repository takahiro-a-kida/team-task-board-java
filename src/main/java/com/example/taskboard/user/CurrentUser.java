package com.example.taskboard.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    public static AppUserDetails get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails u)) {
            throw new IllegalStateException("ログインが必要です");
        }
        return u;
    }

    public static long id() {
        return get().getUserId();
    }
}
