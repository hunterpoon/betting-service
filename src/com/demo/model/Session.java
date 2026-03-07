package com.demo.model;

/**
 * customer session with an expiration time.
 */
public class Session {

    private static final long SESSION_TTL_MS = 10 * 60 * 1000; // 10 minutes

    private final int customerId;
    private final String sessionKey;
    private final long createdTime;

    public Session(int customerId, String sessionKey) {
        this.customerId = customerId;
        this.sessionKey = sessionKey;
        this.createdTime = System.currentTimeMillis();
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdTime > SESSION_TTL_MS;
    }
}
