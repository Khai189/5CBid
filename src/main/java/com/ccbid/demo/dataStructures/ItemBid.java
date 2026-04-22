package com.ccbid.demo.dataStructures;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Map;
import com.ccbid.demo.models.Auctioneer;
import com.ccbid.demo.models.Item;


public interface ItemBid {

    public Auctioneer auctioneer = null;
    public Item item = null;
    public PriorityQueue<Map.Entry<String, Integer>> maxHeap = new PriorityQueue<>((a, b) -> b.getValue().compareTo(a.getValue()));
    public HashMap<String, Integer> hashMap = new HashMap<>();

    public PriorityQueue<Map.Entry<String, Integer>> getPriorityQueue();
    public HashMap<String, Integer> getHashMap();

    public int getHighestBid();
    public String getHighestBidder();

    public void addBid(String bidderId, int bidAmount);
    public void removeBid(String bidderId);

    public void setAuctioneer(Auctioneer auctioneer);
    public void setItem(Item item);

    public Auctioneer getAuctioneer();
    public Item getItem();

}
