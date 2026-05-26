package com.borrowit.service;

import com.borrowit.model.User;
import java.util.Optional;

public final class SessionManager {
    private static User currentUser;

    private SessionManager() {
    }

    public static synchronized void start(User user) {
        currentUser = user;
    }

    public static synchronized Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public static synchronized void clear() {
        currentUser = null;
    }
}
