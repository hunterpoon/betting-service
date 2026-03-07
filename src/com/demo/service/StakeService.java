package com.demo.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages betting stakes storage
 *
 */
public class StakeService {

    private static final int HIGH_STAKES_LIMIT = 20;

    // betOfferId -> (customerId -> CustomerStakes)
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, CustomerStakes>> stakes = new ConcurrentHashMap<>();

    /**
     * Adds a stake
     */
    public void addStake(int betOfferId, int customerId, int stake) {
        stakes.computeIfAbsent(betOfferId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(customerId, k -> new CustomerStakes())
                .addStake(stake);
    }

    /**
     * gets the top 20 high stakes
     * @return CSV
     */
    public String getHighStakes(int betOfferId) {
        ConcurrentHashMap<Integer, CustomerStakes> offerStakes = stakes.get(betOfferId);
        if (offerStakes == null || offerStakes.isEmpty()) {
            return "";
        }

        PriorityQueue<int[]> minHeap = new PriorityQueue<>(
                HIGH_STAKES_LIMIT + 1, Comparator.comparingInt(a -> a[1]));

        for (Map.Entry<Integer, CustomerStakes> entry : offerStakes.entrySet()) {
            int customerId = entry.getKey();
            int maxStake = entry.getValue().getMaxStake();
            if (maxStake > 0) {
                minHeap.offer(new int[] { customerId, maxStake });
                if (minHeap.size() > HIGH_STAKES_LIMIT) {
                    minHeap.poll(); // Remove the smallest
                }
            }
        }

        int[][] items = new int[minHeap.size()][];
        int idx = items.length - 1;
        while (!minHeap.isEmpty()) {
            items[idx--] = minHeap.poll();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(items[i][0]).append("=").append(items[i][1]);
        }
        return sb.toString();
    }

    /**
     * Tracks all stakes for a single customer
     */
    private static class CustomerStakes {
        private final List<Integer> allStakes = new ArrayList<>();
        private final AtomicInteger maxStake = new AtomicInteger(Integer.MIN_VALUE);

        synchronized void addStake(int stake) {
            allStakes.add(stake);
            maxStake.updateAndGet(current -> Math.max(current, stake));
        }

        int getMaxStake() {
            int val = maxStake.get();
            return val == Integer.MIN_VALUE ? 0 : val;
        }
    }
}
