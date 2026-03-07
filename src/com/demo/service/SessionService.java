package com.demo.service;

import com.demo.model.Session;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.logging.Logger;

/**
 * Manages customer sessions
 *
 */
public class SessionService {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    // session key length
    private static final int KEY_LENGTH = 7;
    //generation attempt times
    private static final int MAX_KEY_GENERATION_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    // customerId -> Session
    private final ConcurrentHashMap<Integer, Session> customerSessions = new ConcurrentHashMap<>();
    // sessionKey -> customerId
    private final ConcurrentHashMap<String, Integer> sessionKeyMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Integer, Object> locks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner;

    public SessionService() {
        // session clean
        cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(this::cleanExpiredSessions, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * get or creates a session
     */
    public String getOrCreateSession(int customerId) {
        // Check existing session first
        Session existing = customerSessions.get(customerId);
        if (existing != null && !existing.isExpired()) {
            return existing.getSessionKey();
        }

        // Need to create or replace - synchronize on per-customer basis
        synchronized (getLock(customerId)) {
            // Double-check after acquiring lock
            existing = customerSessions.get(customerId);
            if (existing != null && !existing.isExpired()) {
                return existing.getSessionKey();
            }

            // Remove old session key mapping if exists
            if (existing != null) {
                sessionKeyMap.remove(existing.getSessionKey());
            }

            // Create new session with collision detection
            String sessionKey = generateUniqueSessionKey();
            Session session = new Session(customerId, sessionKey);
            customerSessions.put(customerId, session);
            sessionKeyMap.put(sessionKey, customerId);
            return sessionKey;
        }
    }

    /**
     * validate a session key and returns customerId, if not exists returns -1
     */
    public int validateSessionKey(String sessionKey) {
        Integer customerId = sessionKeyMap.get(sessionKey);
        if (customerId == null) {
            return -1;
        }

        Session session = customerSessions.get(customerId);
        if (session == null || session.isExpired() || !session.getSessionKey().equals(sessionKey)) {
            return -1;
        }

        return customerId;
    }

    private void cleanExpiredSessions() {
        int cleaned = 0;
        for (Map.Entry<Integer, Session> entry : customerSessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                Session removed = customerSessions.remove(entry.getKey());
                if (removed != null) {
                    sessionKeyMap.remove(removed.getSessionKey());
                    locks.remove(entry.getKey());
                    cleaned++;
                }
            }
        }
        if (cleaned > 0) {
            System.out.println("Cleaned " + cleaned + " expired session");
        }
    }

    /**
     * generate a unique session key
     */
    private String generateUniqueSessionKey() {
        for (int i = 0; i < MAX_KEY_GENERATION_ATTEMPTS; i++) {
            String key = generateSessionKey();
            if (!sessionKeyMap.containsKey(key)) {
                return key;
            }
            System.out.println("retrying (attempt " + (i + 1) + ")");
        }
        throw new IllegalStateException("failed to generate unique session key");
    }

    private String generateSessionKey() {
        StringBuilder sb = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * get lock
     */
    private Object getLock(int customerId) {
        return locks.computeIfAbsent(customerId, k -> new Object());
    }

    public void shutdown() {
        cleaner.shutdownNow();
    }
}
