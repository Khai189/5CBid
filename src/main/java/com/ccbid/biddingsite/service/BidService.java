package com.ccbid.biddingsite.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccbid.biddingsite.dataStructures.ItemBid;
import com.ccbid.biddingsite.dto.ActiveBidSummaryResponse;
import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.Bid;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.models.Bidder;
import com.ccbid.biddingsite.repository.AuctioneerRepo;
import com.ccbid.biddingsite.repository.BidRepo;
import com.ccbid.biddingsite.repository.BidderRepo;
import com.ccbid.biddingsite.repository.ItemRepo;

import jakarta.annotation.PostConstruct;

@Service
public class BidService {

    private final Map<String, ItemBid> itemBids = new ConcurrentHashMap<>();

    @Autowired private ItemRepo itemRepo;
    @Autowired private AuctioneerRepo auctioneerRepo;
    @Autowired private BidRepo bidRepo;
    @Autowired private BidderRepo bidderRepo;

    @PostConstruct
    @Transactional(readOnly = true)
    public void rehydrate() {
        for (BidItem item : itemRepo.findAll()) {
            itemBids.put(item.getItemId(), new ItemBid(item.getAuctioneer(), item));
        }
        for (Bid b : bidRepo.findAllByActiveTrueOrderByCreatedAtAsc()) {
            ItemBid ib = itemBids.get(b.getItemId());
            if (ib != null) {
                ib.addBid(b.getBidderId(), b.getAmount());
            }
        }
    }

    @Transactional
    public ItemBid addItem(BidItem item, Auctioneer auctioneer) {
        if (item == null || item.getItemId() == null) {
            throw new IllegalArgumentException("Item and itemId are required");
        }
        if (auctioneer == null || auctioneer.getAuctioneerId() == null) {
            throw new IllegalArgumentException("An auctioneer is required to list an item");
        }
        if (itemBids.containsKey(item.getItemId()) || itemRepo.existsById(item.getItemId())) {
            throw new IllegalStateException("Item " + item.getItemId() + " is already listed");
        }
        Auctioneer persistedAuctioneer = auctioneerRepo.findById(auctioneer.getAuctioneerId())
            .orElseGet(() -> auctioneerRepo.save(auctioneer));
        item.setAuctioneer(persistedAuctioneer);
        BidItem savedItem = itemRepo.save(item);
        ItemBid ib = new ItemBid(persistedAuctioneer, savedItem);
        ItemBid existing = itemBids.putIfAbsent(savedItem.getItemId(), ib);
        if (existing != null) {
            throw new IllegalStateException("Item " + savedItem.getItemId() + " is already listed");
        }
        return ib;
    }

    public ItemBid addItem(String itemId, String itemName, Integer startingPrice,
                           String description, String auctioneerId, String auctioneerName) {
        BidItem item = new BidItem();
        item.setItemId(itemId);
        item.setItemName(itemName);
        item.setStartingPrice(startingPrice);
        item.setDescription(description);
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

    @Transactional
    public String placeBid(String itemId, String bidderId, int amount) {
        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("bidderId is required");
        }
        bidderRepo.findById(bidderId)
            .orElseThrow(() -> new IllegalStateException("Bidder " + bidderId + " is not registered"));
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
            bidRepo.save(new Bid(null, itemId, bidderId, amount, Instant.now(), true));
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

    @Transactional(readOnly = true)
    public List<ActiveBidSummaryResponse> getActiveBidSummariesForBidder(String bidderId) {
        List<ActiveBidSummaryResponse> summaries = new ArrayList<>();
        List<String> itemIds = bidRepo.findAllByBidderIdAndActiveTrueOrderByCreatedAtAsc(bidderId).stream()
            .map(Bid::getItemId)
            .distinct()
            .sorted()
            .toList();

        for (String itemId : itemIds) {
            ItemBid itemBid = itemBids.get(itemId);
            if (itemBid != null) {
                summaries.add(toSummary(itemBid, bidderId, false));
            }
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public List<ActiveBidSummaryResponse> getActiveBidSummariesForAuctioneer(String auctioneerId) {
        List<ActiveBidSummaryResponse> summaries = new ArrayList<>();
        for (BidItem item : itemRepo.findAllByAuctioneer_AuctioneerIdOrderByItemIdAsc(auctioneerId)) {
            ItemBid itemBid = itemBids.get(item.getItemId());
            if (itemBid != null) {
                summaries.add(toSummary(itemBid, null, true));
            }
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public List<ActiveBidSummaryResponse> getAllActiveBidSummaries() {
        return itemRepo.findAll().stream()
            .map(BidItem::getItemId)
            .sorted()
            .map(itemBids::get)
            .filter(itemBid -> itemBid != null)
            .map(itemBid -> toSummary(itemBid, null, false))
            .toList();
    }

    @Transactional
    public String removeBid(String itemId, String bidderId) {
        ItemBid bid = getItem(itemId);
        synchronized (bid) {
            List<Bid> active = bidRepo.findAllByItemIdAndBidderIdAndActiveTrue(itemId, bidderId);
            for (Bid b : active) {
                b.setActive(false);
            }
            bidRepo.saveAll(active);
            bid.removeBid(bidderId);
            return "Removed bid by " + bidderId + " on " + itemId;
        }
    }

    private ActiveBidSummaryResponse toSummary(ItemBid itemBid, String viewerBidderId, boolean viewerOwnsListing) {
        BidItem item = itemBid.getItem();
        Auctioneer auctioneer = itemBid.getAuctioneer();
        String highestBidderId = itemBid.getHighestBidder();
        int highestBidAmount = itemBid.getHighestBid();
        Integer viewerBidAmount = viewerBidderId == null ? null : itemBid.getHashMap().get(viewerBidderId);

        return new ActiveBidSummaryResponse(
            item.getItemId(),
            item.getItemName(),
            item.getDescription(),
            item.getStartingPrice(),
            auctioneer == null ? null : auctioneer.getAuctioneerId(),
            auctioneer == null ? null : auctioneer.getName(),
            highestBidderId,
            highestBidAmount < 0 ? null : highestBidAmount,
            viewerBidAmount,
            viewerBidAmount != null && viewerBidAmount.equals(highestBidAmount),
            viewerOwnsListing
        );
    }
}
