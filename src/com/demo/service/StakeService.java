package com.demo.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages betting stakes storage
 *
 */
public class StakeService {

    private static final int HIGH_STAKES_LIMIT = 20;

    // betOfferId -> OfferStakes
    private final ConcurrentHashMap<Integer, OfferStakes> stakes = new ConcurrentHashMap<>();

    /**
     * Adds a stake
     */
    public void addStake(int betOfferId, int customerId, int stake) {
        stakes.computeIfAbsent(betOfferId, k -> new OfferStakes())
                .addStake(customerId, stake);
    }

    /**
     * gets the top 20 high stakes
     * 
     * @return CSV
     */
    public String getHighStakes(int betOfferId) {
        OfferStakes offerStakes = stakes.get(betOfferId);
        if (offerStakes == null) {
            return "";
        }
        return offerStakes.getHighStakes();
    }

    private static class OfferStakes {
        private final ConcurrentHashMap<Integer, CustomerStakes> customerStakes = new ConcurrentHashMap<>();

        // cache high stakes string
        private volatile String cachedHighStakes = "";
        // recalculate flag
        private volatile boolean dirty = true;
        private final Object lock = new Object();

        public void addStake(int customerId, int stake) {
            CustomerStakes cs = customerStakes.computeIfAbsent(customerId, k -> new CustomerStakes());
            if (cs.addStake(stake)) {
                //setup recalculate flag
                dirty = true;
            }
        }

        public String getHighStakes() {
            if (!dirty) {
                return cachedHighStakes;
            }
            synchronized (lock) {
                if (!dirty) {
                    return cachedHighStakes;
                }
                // if dirty is true, recalculate
                cachedHighStakes = calculateHighStakes();
                dirty = false;
                return cachedHighStakes;
            }
        }

        private String calculateHighStakes() {
            if (customerStakes.isEmpty()) {
                return "";
            }

            PriorityQueue<int[]> minHeap = new PriorityQueue<>(
                    HIGH_STAKES_LIMIT + 1, Comparator.comparingInt(a -> a[1]));

            for (Map.Entry<Integer, CustomerStakes> entry : customerStakes.entrySet()) {
                int customerId = entry.getKey();
                int maxStake = entry.getValue().getMaxStake();
                if (maxStake > 0) {
                    minHeap.offer(new int[] { customerId, maxStake });
                    if (minHeap.size() > HIGH_STAKES_LIMIT) {
                        minHeap.poll(); // Remove the smallest
                    }
                }
            }

            if (minHeap.isEmpty()) {
                return "";
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
    }

    /**
     * Tracks all stakes for a single customer
     */
    private static class CustomerStakes {
        private final List<Integer> allStakes = new ArrayList<>();
        private int maxStake = 0;

        synchronized boolean addStake(int stake) {
            allStakes.add(stake);
            if (stake > maxStake) {
                maxStake = stake;
                return true;
            }
            return false;
        }

        synchronized int getMaxStake() {
            return maxStake;
        }
    }
}
