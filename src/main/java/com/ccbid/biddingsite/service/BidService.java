package com.ccbid.biddingsite.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccbid.biddingsite.dataStructures.ItemBid;
import com.ccbid.biddingsite.dto.ActiveBidSummaryResponse;
import com.ccbid.biddingsite.dto.BidHistorySummaryResponse;
import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.Bid;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.models.Bidder;
import com.ccbid.biddingsite.models.ItemCondition;
import com.ccbid.biddingsite.models.ListingDurationUnit;
import com.ccbid.biddingsite.repository.AuctioneerRepo;
import com.ccbid.biddingsite.repository.BidRepo;
import com.ccbid.biddingsite.repository.BidderRepo;
import com.ccbid.biddingsite.repository.ItemRepo;

import jakarta.annotation.PostConstruct;

@Service
public class BidService {
    private static final Duration DEFAULT_LISTING_DURATION = Duration.ofDays(7);

    private final Map<String, ItemBid> itemBids = new ConcurrentHashMap<>();

    @Autowired private ItemRepo itemRepo;
    @Autowired private AuctioneerRepo auctioneerRepo;
    @Autowired private BidRepo bidRepo;
    @Autowired private BidderRepo bidderRepo;

    @PostConstruct
    @Transactional(readOnly = true)
    public void rehydrate() {
        expireStaleListings();
        itemBids.clear();
        for (BidItem item : itemRepo.findAllByArchivedFalseOrderByItemIdAsc()) {
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
        expireStaleListings();
        if (item == null) {
            throw new IllegalArgumentException("Item is required");
        }
        if (item.getItemName() == null || item.getItemName().isBlank()) {
            throw new IllegalArgumentException("itemName is required");
        }
        if (item.getItemId() == null || item.getItemId().isBlank()) {
            item.setItemId(generateItemId(item.getItemName()));
        }
        if (item.getStartingPrice() == null || item.getStartingPrice() < 0.5d) {
            throw new IllegalArgumentException("Price must be equal to or above 50 cents");
        }
        if (item.getCondition() == null) {
            throw new IllegalArgumentException("condition is required");
        }
        if (item.getExpiresAt() == null) {
            item.setExpiresAt(Instant.now().plus(DEFAULT_LISTING_DURATION));
        }
        if (!item.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Listing expiration must be in the future");
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

    public ItemBid addItem(String itemName, Double startingPrice,
                           String description, ItemCondition condition,
                           Integer durationAmount, ListingDurationUnit durationUnit,
                           String auctioneerId, String auctioneerName) {
        BidItem item = new BidItem();
        item.setItemName(itemName);
        item.setStartingPrice(startingPrice);
        item.setDescription(description);
        item.setCondition(condition);
        item.setArchived(false);
        item.setExpiresAt(computeExpiresAt(durationAmount, durationUnit));
        Auctioneer auctioneer = new Auctioneer();
        auctioneer.setAuctioneerId(auctioneerId);
        auctioneer.setName(auctioneerName);
        return addItem(item, auctioneer);
    }

    public ItemBid getItem(String itemId) {
        expireStaleListings();
        ItemBid bid = itemBids.get(itemId);
        if (bid == null) {
            throw new IllegalStateException("Item " + itemId + " has not been listed by an auctioneer");
        }
        return bid;
    }

    @Transactional
    public String placeBid(String itemId, String bidderId, int amount) {
        expireStaleListings();
        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("bidderId is required");
        }
        itemRepo.findByItemId(itemId)
            .orElseThrow(() -> new IllegalStateException("Item " + itemId + " has not been listed by an auctioneer"));
        bidderRepo.findById(bidderId)
            .orElseThrow(() -> new IllegalStateException("Bidder " + bidderId + " is not registered"));
        ItemBid bid = getItem(itemId);
        synchronized (bid) {
            int currentHighest = bid.getHighestBid();
            String currentLeader = bid.getHighestBidder();
            Double startingPrice = bid.getItem().getStartingPrice();
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
        expireStaleListings();
        return getItem(itemId).getHighestBid();
    }

    public String getHighestBidder(String itemId) {
        expireStaleListings();
        return getItem(itemId).getHighestBidder();
    }

    @Transactional(readOnly = true)
    public int getActiveBidCount(String itemId) {
        expireStaleListings();
        return Math.toIntExact(bidRepo.countByItemIdAndActiveTrue(itemId));
    }

    @Transactional(readOnly = true)
    public ItemBid getLiveItemBidOrNull(String itemId) {
        return itemBids.get(itemId);
    }

    @Transactional(readOnly = true)
    public List<ActiveBidSummaryResponse> getActiveBidSummariesForBidder(String bidderId) {
        expireStaleListings();
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
        expireStaleListings();
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
        expireStaleListings();
        return itemRepo.findAll().stream()
            .map(BidItem::getItemId)
            .sorted()
            .map(itemBids::get)
            .filter(itemBid -> itemBid != null)
            .map(itemBid -> toSummary(itemBid, null, false))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BidHistorySummaryResponse> getBidHistoryForBidder(String bidderId) {
        expireStaleListings();
        LinkedHashSet<String> itemIds = bidRepo.findAllByBidderIdAndActiveFalseOrderByCreatedAtAsc(bidderId).stream()
            .map(Bid::getItemId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return itemIds.stream()
            .map(itemId -> toHistorySummary(itemId, bidderId, false))
            .filter(summary -> summary != null)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BidHistorySummaryResponse> getBidHistoryForAuctioneer(String auctioneerId) {
        expireStaleListings();
        return itemRepo.findAllByAuctioneer_AuctioneerIdOrderByItemIdAsc(auctioneerId).stream()
            .filter(BidItem::isArchived)
            .map(item -> toHistorySummary(item.getItemId(), null, true))
            .filter(summary -> summary != null)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BidHistorySummaryResponse> getAllBidHistory() {
        expireStaleListings();
        return itemRepo.findAll().stream()
            .filter(BidItem::isArchived)
            .sorted(Comparator.comparing(BidItem::getItemId))
            .map(item -> toHistorySummary(item.getItemId(), null, false))
            .filter(summary -> summary != null)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BidHistorySummaryResponse> getExpiredBidOutcomes() {
        expireStaleListings();
        return getAllBidHistory();
    }

    @Transactional
    public String removeBid(String itemId, String bidderId) {
        expireStaleListings();
        itemRepo.findByItemId(itemId)
            .orElseThrow(() -> new IllegalStateException("Item " + itemId + " has not been listed by an auctioneer"));
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

    @Transactional
    public void expireStaleListings() {
        Instant now = Instant.now();
        for (BidItem item : itemRepo.findAllByArchivedFalseOrderByItemIdAsc()) {
            if (item.getExpiresAt() == null) {
                item.setExpiresAt(now.plus(DEFAULT_LISTING_DURATION));
                itemRepo.save(item);
            }
            if (item.getExpiresAt() != null && !item.getExpiresAt().isAfter(now)) {
                archiveListing(item);
            }
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
            item.getCondition() == null ? null : item.getCondition().name(),
            auctioneer == null ? null : auctioneer.getAuctioneerId(),
            auctioneer == null ? null : auctioneer.getName(),
            highestBidderId,
            highestBidAmount < 0 ? null : highestBidAmount,
            viewerBidAmount,
            viewerBidAmount != null && viewerBidAmount.equals(highestBidAmount),
            viewerOwnsListing
        );
    }

    private BidHistorySummaryResponse toHistorySummary(String itemId, String viewerBidderId, boolean viewerOwnsListing) {
        BidItem item = itemRepo.findById(itemId).orElse(null);
        if (item == null) {
            return null;
        }

        List<Bid> bids = bidRepo.findAllByItemIdOrderByCreatedAtAsc(itemId).stream()
            .filter(bid -> !bid.isActive())
            .toList();
        if (bids.isEmpty()) {
            return null;
        }

        Bid highestBid = bids.stream()
            .max(Comparator.comparing(Bid::getAmount).thenComparing(Bid::getCreatedAt))
            .orElse(null);
        Instant lastBidAt = bids.stream()
            .map(Bid::getCreatedAt)
            .max(Comparator.naturalOrder())
            .orElse(null);
        Integer viewerBidAmount = viewerBidderId == null ? null : bids.stream()
            .filter(bid -> bid.getBidderId().equals(viewerBidderId))
            .map(Bid::getAmount)
            .max(Integer::compareTo)
            .orElse(null);

        Auctioneer auctioneer = item.getAuctioneer();
        return new BidHistorySummaryResponse(
            item.getItemId(),
            item.getItemName(),
            item.getDescription(),
            item.getStartingPrice(),
            item.getCondition() == null ? null : item.getCondition().name(),
            auctioneer == null ? null : auctioneer.getAuctioneerId(),
            auctioneer == null ? null : auctioneer.getName(),
            highestBid == null ? null : highestBid.getBidderId(),
            highestBid == null ? null : highestBid.getAmount(),
            viewerBidAmount,
            lastBidAt,
            bids.size(),
            viewerOwnsListing
        );
    }

    private String generateItemId(String itemName) {
        String slug = itemName == null ? "" : itemName.trim().toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            slug = "listing";
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
            if (!itemBids.containsKey(candidate) && !itemRepo.existsById(candidate)) {
                return candidate;
            }
        }

        return "listing-" + UUID.randomUUID();
    }

    private Instant computeExpiresAt(Integer durationAmount, ListingDurationUnit durationUnit) {
        if (durationAmount == null || durationAmount < 1) {
            throw new IllegalArgumentException("durationAmount must be at least 1");
        }
        if (durationUnit == null) {
            throw new IllegalArgumentException("durationUnit is required");
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return switch (durationUnit) {
            case HOURS -> now.plusHours(durationAmount).toInstant();
            case DAYS -> now.plusDays(durationAmount).toInstant();
            case WEEKS -> now.plusWeeks(durationAmount).toInstant();
            case MONTHS -> now.plusMonths(durationAmount).toInstant();
        };
    }

    private void archiveListing(BidItem item) {
        item.setArchived(true);
        itemRepo.save(item);
        List<Bid> activeBids = bidRepo.findAllByItemIdAndActiveTrueOrderByCreatedAtAsc(item.getItemId());
        for (Bid bid : activeBids) {
            bid.setActive(false);
        }
        if (!activeBids.isEmpty()) {
            bidRepo.saveAll(activeBids);
        }
        itemBids.remove(item.getItemId());
    }
}
