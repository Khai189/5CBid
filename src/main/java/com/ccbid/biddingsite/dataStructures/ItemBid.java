package com.ccbid.biddingsite.dataStructures;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.BidItem;

public class ItemBid {
    /**
     * This class tracks bids by combining a HashMap for 
     * quick lookup and a PriorityQueue (Max-Heap) to get the highest bid 
     * hashMap: Stores the most recent bid for each bidderId 
     * maxHeap: priority queue that stores bid entries by bidAmount in descending order
     */

    private Auctioneer auctioneer;
    private BidItem item;
    private final PriorityQueue<Map.Entry<String, Integer>> maxHeap =
        new PriorityQueue<>((a, b) -> b.getValue().compareTo(a.getValue()));
    private final HashMap<String, Integer> hashMap = new HashMap<>();

    public ItemBid() {
    }

    public ItemBid(Auctioneer auctioneer, BidItem item) {
        this.auctioneer = auctioneer;
        this.item = item;
    }


    public PriorityQueue<Map.Entry<String, Integer>> getPriorityQueue() {
        return maxHeap;
    }

    public HashMap<String, Integer> getHashMap() {
        return hashMap;
    }

    /**
    * Adds/updates bids for a specific user 
    * @param bidderId (String): unique identifier for bidder 
    * @param bidAmount (Integer): value of the bid 
    */
    public synchronized void addBid(String bidderId, int bidAmount) {
        hashMap.put(bidderId, bidAmount);
        maxHeap.offer(new AbstractMap.SimpleEntry<>(bidderId, bidAmount));
    }

    /**
     * Removes bid from specific bidder 
     * @param bidderId (String): unique identifier for bidder
     */
    public synchronized void removeBid(String bidderId) {
        hashMap.remove(bidderId);
    }

    /**
     * Retrieves the current highest bid by getting what is at the top of the heap
     * @return The highest bid amount, or -1 if there are no bids 
     */
    public synchronized int getHighestBid() {
        Map.Entry<String, Integer> top = peekValid();
        return top == null ? -1 : top.getValue();
    }

    public synchronized String getHighestBidder() {
        Map.Entry<String, Integer> top = peekValid();
        return top == null ? null : top.getKey();
    }

    /**
     * Retrieves the current highest bid from the root of the max-heap 
     * @return The valid highest bidder and their amount, or null if no valid bids remain
     */
    private Map.Entry<String, Integer> peekValid() {
        while (!maxHeap.isEmpty()) {
            Map.Entry<String, Integer> top = maxHeap.peek();
            Integer current = hashMap.get(top.getKey());
            if (current != null && current.equals(top.getValue())) {
                return top;
            }
            maxHeap.poll();
        }
        return null;
    }

    public void setAuctioneer(Auctioneer auctioneer) {
        this.auctioneer = auctioneer;
    }

    public void setItem(BidItem item) {
        this.item = item;
    }

    public Auctioneer getAuctioneer() {
        return auctioneer;
    }

    public BidItem getItem() {
        return item;
    }
}
