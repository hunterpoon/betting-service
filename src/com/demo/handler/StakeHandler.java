package com.demo.handler;

import com.demo.service.SessionService;
import com.demo.service.StakeService;

/**
 * Handle stake stake logic
 */
public class StakeHandler {

    private final SessionService sessionService;
    private final StakeService stakeService;

    public StakeHandler(SessionService sessionService, StakeService stakeService) {
        this.sessionService = sessionService;
        this.stakeService = stakeService;
    }

    public enum StakeResult {
        INVALID_SESSION,
        ACCEPTED
    }

    /**
     * Submits a stake
     *
     * @param betOfferId
     * @param sessionKey
     * @param stake
     * @return accept or refuse
     * @throws IllegalArgumentException
     */
    public StakeResult handle(int betOfferId, String sessionKey, int stake) {
        if (betOfferId <= 0) {
            throw new IllegalArgumentException("Bet offer ID must be greater than 0, betOfferId: " + betOfferId);
        }
        if (sessionKey == null || sessionKey.isEmpty()) {
            throw new IllegalArgumentException("Session key must not be null or empty");
        }
        if (stake <= 0) {
            throw new IllegalArgumentException("Stake must be greater than 0, stake: " + stake);
        }

        int customerId = sessionService.validateSessionKey(sessionKey);
        if (customerId < 0) {
            return StakeResult.INVALID_SESSION;
        }

        stakeService.addStake(betOfferId, customerId, stake);
        return StakeResult.ACCEPTED;
    }
}
