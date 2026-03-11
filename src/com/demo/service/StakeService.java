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
        // customerId -> maxStake
        private final ConcurrentHashMap<Integer, Integer> customerMaxStakes = new ConcurrentHashMap<>();

        // cache high stakes string
        private volatile String cachedHighStakes = "";
        // recalculate flag
        private volatile boolean dirty = true;
        private final Object lock = new Object();

        public void addStake(int customerId, int stake) {
            Integer current = customerMaxStakes.get(customerId);
            if (current == null || stake > current) {
                customerMaxStakes.put(customerId, stake);
            }
            dirty = true;
            pruneIfNeeded();
        }

        /**
         * if customerMaxStakes size > HIGH_STAKES_LIMIT, cut the results
         */
        private void pruneIfNeeded() {
            if (customerMaxStakes.size() <= HIGH_STAKES_LIMIT) {
                return;
            }
            // cut results
            List<Integer> values = new ArrayList<>(customerMaxStakes.values());
            values.sort(Collections.reverseOrder());
            int threshold = values.get(HIGH_STAKES_LIMIT - 1);
            Iterator<Map.Entry<Integer, Integer>> it = customerMaxStakes.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue() < threshold) {
                    it.remove();
                }
            }
        }

        public String getHighStakes() {
            if (!dirty) {
                return cachedHighStakes;
            }
            //update the cache
            synchronized (lock) {
                if (!dirty) {
                    return cachedHighStakes;
                }
                cachedHighStakes = calculateHighStakes();
                dirty = false;
                return cachedHighStakes;
            }
        }

        private String calculateHighStakes() {
            if (customerMaxStakes.isEmpty()) {
                return "";
            }

            PriorityQueue<int[]> minHeap = new PriorityQueue<>(
                    HIGH_STAKES_LIMIT + 1, Comparator.comparingInt(a -> a[1]));

            for (Map.Entry<Integer, Integer> entry : customerMaxStakes.entrySet()) {
                int customerId = entry.getKey();
                int maxStake = entry.getValue();
                if (maxStake > 0) {
                    minHeap.offer(new int[] { customerId, maxStake });
                    if (minHeap.size() > HIGH_STAKES_LIMIT) {
                        minHeap.poll();
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
}
