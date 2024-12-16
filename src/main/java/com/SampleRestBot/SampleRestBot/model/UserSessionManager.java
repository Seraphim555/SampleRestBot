package com.SampleRestBot.SampleRestBot.model;

import java.util.HashMap;
import java.util.Map;

public class UserSessionManager {
    private static final Map<Long, UserSession> userSessions = new HashMap<>();

    // Получить сессию пользователя
    public static UserSession getUserSession(long chatId) {
        return userSessions.computeIfAbsent(chatId, UserSession::new);
    }

    // Удалить сессию пользователя
    public static void removeUserSession(long chatId) {
        userSessions.remove(chatId);
    }
}

