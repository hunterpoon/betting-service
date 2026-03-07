package com.demo.handler;

import com.demo.service.SessionService;

/**
 * handle session creation logic
 */
public class SessionHandler {

    private final SessionService sessionService;

    public SessionHandler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * get or create a session for customer
     *
     * @param customerId
     * @return session key
     * @throws IllegalArgumentException
     */
    public String handle(int customerId) {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must greater than 0, customerId: " + customerId);
        }
        return sessionService.getOrCreateSession(customerId);
    }
}
