package com.ccbid.biddingsite.dataStructures;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.BidItem;

public class ItemBid {

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

    public synchronized void addBid(String bidderId, int bidAmount) {
        hashMap.put(bidderId, bidAmount);
        maxHeap.offer(new AbstractMap.SimpleEntry<>(bidderId, bidAmount));
    }

    public synchronized void removeBid(String bidderId) {
        hashMap.remove(bidderId);
    }

    public synchronized int getHighestBid() {
        Map.Entry<String, Integer> top = peekValid();
        return top == null ? -1 : top.getValue();
    }

    public synchronized String getHighestBidder() {
        Map.Entry<String, Integer> top = peekValid();
        return top == null ? null : top.getKey();
    }

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
