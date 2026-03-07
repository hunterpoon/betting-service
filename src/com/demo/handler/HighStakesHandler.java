package com.demo.handler;

import com.demo.service.StakeService;

/**
 * Handle highstakes logic
 */
public class HighStakesHandler {

    private final StakeService stakeService;

    public HighStakesHandler(StakeService stakeService) {
        this.stakeService = stakeService;
    }

    /**
     * get the top 20 highest stakes
     *
     * @param betOfferId
     * @return CSV
     * @throws IllegalArgumentException
     */
    public String handle(int betOfferId) {
        if (betOfferId <= 0) {
            throw new IllegalArgumentException("Bet offer ID must be greater than 0, betOfferId: " + betOfferId);
        }
        return stakeService.getHighStakes(betOfferId);
    }
}
