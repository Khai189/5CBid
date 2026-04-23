package com.ccbid.biddingsite.dataStructures;
import java.util.HashMap;
import java.util.PriorityQueue;

import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.BidItem;

import java.util.Map;


public interface ItemBid {

    public Auctioneer auctioneer = null;
    public BidItem item = null;
    public PriorityQueue<Map.Entry<String, Integer>> maxHeap = new PriorityQueue<>((a, b) -> b.getValue().compareTo(a.getValue()));
    public HashMap<String, Integer> hashMap = new HashMap<>();

    public PriorityQueue<Map.Entry<String, Integer>> getPriorityQueue();
    public HashMap<String, Integer> getHashMap();

    public int getHighestBid();
    public String getHighestBidder();

    public void addBid(String bidderId, int bidAmount);
    public void removeBid(String bidderId);

    public void setAuctioneer(Auctioneer auctioneer);
    public void setItem(BidItem item);

    public Auctioneer getAuctioneer();
    public BidItem getItem();

}
