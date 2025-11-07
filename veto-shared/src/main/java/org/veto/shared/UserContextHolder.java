package org.veto.shared;

import lombok.Data;

public class UserContextHolder {
    private static final ThreadLocal<UserContext> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUser(UserContext user) {
        USER_THREAD_LOCAL.set(user);
    }

    public static UserContext getUser() {
        return USER_THREAD_LOCAL.get();
    }

    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }

    @Data
    public static class UserContext {
        private Long id;

        private String nickname;

        private String token;
    }
}