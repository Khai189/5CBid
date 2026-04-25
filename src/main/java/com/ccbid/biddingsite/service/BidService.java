package com.ccbid.biddingsite.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.ccbid.biddingsite.dataStructures.ItemBid;
import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.BidItem;

@Service
public class BidService {

    private final Map<String, ItemBid> itemBids = new ConcurrentHashMap<>();

    public ItemBid addItem(BidItem item, Auctioneer auctioneer) {
        if (item == null || item.getItemId() == null) {
            throw new IllegalArgumentException("Item and itemId are required");
        }
        if (auctioneer == null || auctioneer.getAuctioneerId() == null) {
            throw new IllegalArgumentException("An auctioneer is required to list an item");
        }
        ItemBid existing = itemBids.putIfAbsent(item.getItemId(), new ItemBid(auctioneer, item));
        if (existing != null) {
            throw new IllegalStateException("Item " + item.getItemId() + " is already listed");
        }
        return itemBids.get(item.getItemId());
    }

    public ItemBid addItem(String itemId, String itemName, Integer startingPrice,
                           String auctioneerId, String auctioneerName) {
        BidItem item = new BidItem();
        item.setItemId(itemId);
        item.setItemName(itemName);
        item.setStartingPrice(startingPrice);
        Auctioneer auctioneer = new Auctioneer();
        auctioneer.setAuctioneerId(auctioneerId);
        auctioneer.setName(auctioneerName);
        return addItem(item, auctioneer);
    }

    public ItemBid getItem(String itemId) {
        ItemBid bid = itemBids.get(itemId);
        if (bid == null) {
            throw new IllegalStateException("Item " + itemId + " has not been listed by an auctioneer");
        }
        return bid;
    }

    public String placeBid(String itemId, String bidderId, int amount) {
        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("bidderId is required");
        }
        ItemBid bid = getItem(itemId);
        synchronized (bid) {
            int currentHighest = bid.getHighestBid();
            String currentLeader = bid.getHighestBidder();
            Integer startingPrice = bid.getItem().getStartingPrice();
            if (bidderId.equals(currentLeader)) {
                throw new IllegalArgumentException("Bidder " + bidderId + " already holds the highest bid and cannot rebid");
            }
            if (currentHighest < 0 && startingPrice != null && amount < startingPrice) {
                throw new IllegalArgumentException("Bid " + amount + " is below starting price " + startingPrice);
            }
            if (currentHighest >= 0 && amount <= currentHighest) {
                throw new IllegalArgumentException("Bid " + amount + " must be greater than current max " + currentHighest);
            }
            bid.addBid(bidderId, amount);
            return "Bid placed: bidder=" + bidderId + " amount=" + amount + " item=" + itemId
                + " | currentHighest=" + bid.getHighestBid() + " by " + bid.getHighestBidder();
        }
    }

    public int getHighestBid(String itemId) {
        return getItem(itemId).getHighestBid();
    }

    public String getHighestBidder(String itemId) {
        return getItem(itemId).getHighestBidder();
    }

    public String removeBid(String itemId, String bidderId) {
        ItemBid bid = getItem(itemId);
        bid.removeBid(bidderId);
        return "Removed bid by " + bidderId + " on " + itemId;
    }
}
