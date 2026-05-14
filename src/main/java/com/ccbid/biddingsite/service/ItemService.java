package com.ccbid.biddingsite.service;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import com.ccbid.biddingsite.dataStructures.ItemBid;
import com.ccbid.biddingsite.dto.ItemListingSummaryResponse;
import com.ccbid.biddingsite.models.Auctioneer;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.models.ItemCondition;
import com.ccbid.biddingsite.repository.ItemRepo;

@Service
public class ItemService {
    @Autowired
    private ItemRepo repo;
    @Autowired
    private BidService bidService;

    public List<ItemListingSummaryResponse> getItems(String query, String auctioneerId, Double minPrice, Double maxPrice, String condition) {
        bidService.expireStaleListings();
        if (maxPrice != null && maxPrice < 0.5d) {
            throw new IllegalArgumentException("maxPrice must be at least 0.5");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }
        ItemCondition normalizedCondition = parseCondition(condition);
        String normalizedQuery = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        String normalizedAuctioneerId = auctioneerId == null ? null : auctioneerId.trim().toLowerCase(Locale.ROOT);

        return repo.findAllByArchivedFalseOrderByItemIdAsc().stream()
            .filter(item -> matchesQuery(item, normalizedQuery))
            .filter(item -> matchesAuctioneer(item, normalizedAuctioneerId))
            .filter(item -> matchesMinPrice(item, minPrice))
            .filter(item -> matchesMaxPrice(item, maxPrice))
            .filter(item -> normalizedCondition == null || item.getCondition() == normalizedCondition)
            .map(this::toSummary)
            .toList();
    }

    public ItemListingSummaryResponse getItem(String itemId) {
        bidService.expireStaleListings();
        BidItem item = getItemEntity(itemId);
        if (item.isArchived()) {
            throw new IllegalStateException("Item " + itemId + " not found");
        }
        return toSummary(item);
    }

    public BidItem updateItem(String itemId, String itemName, Double startingPrice){
        BidItem item = getItemEntity(itemId);
        item.setItemName(itemName);
        item.setStartingPrice(startingPrice);
        return repo.save(item);
    }

    public HttpStatus addItem(String itemId, String itemName, Double startingPrice){
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }
        if (repo.existsById(itemId)) {
            throw new IllegalStateException("Item " + itemId + " already exists");
        }
        BidItem item = new BidItem();
        item.setItemId(itemId);
        item.setItemName(itemName);
        item.setStartingPrice(startingPrice);
        repo.save(item);
        return HttpStatus.CREATED;
    }

    public HttpStatus deleteItem(String itemId){
        if (!repo.existsById(itemId)) {
            throw new IllegalStateException("Item " + itemId + " does not exist");
        }
        repo.deleteById(itemId);
        return HttpStatus.OK;
    }

    private boolean matchesQuery(BidItem item, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return contains(item.getItemId(), query)
            || contains(item.getItemName(), query)
            || contains(item.getDescription(), query)
            || contains(item.getAuctioneer() == null ? null : item.getAuctioneer().getAuctioneerId(), query)
            || contains(item.getAuctioneer() == null ? null : item.getAuctioneer().getName(), query)
            || contains(item.getCondition() == null ? null : item.getCondition().name().replace('_', ' '), query);
    }

    private boolean matchesAuctioneer(BidItem item, String auctioneerId) {
        if (auctioneerId == null || auctioneerId.isBlank()) {
            return true;
        }
        return item.getAuctioneer() != null
            && item.getAuctioneer().getAuctioneerId() != null
            && item.getAuctioneer().getAuctioneerId().toLowerCase(Locale.ROOT).equals(auctioneerId);
    }

    private boolean matchesMinPrice(BidItem item, Double minPrice) {
        return minPrice == null || item.getStartingPrice() == null || item.getStartingPrice() >= minPrice;
    }

    private boolean matchesMaxPrice(BidItem item, Double maxPrice) {
        return maxPrice == null || item.getStartingPrice() == null || item.getStartingPrice() <= maxPrice;
    }

    private ItemCondition parseCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return null;
        }
        try {
            return ItemCondition.valueOf(condition.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("condition must be NEW, USED, or HIGHLY_DAMAGED");
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private BidItem getItemEntity(String itemId) {
        return repo.findById(itemId)
            .orElseThrow(() -> new IllegalStateException("Item " + itemId + " not found"));
    }

    private ItemListingSummaryResponse toSummary(BidItem item) {
        ItemBid liveBid = item.isArchived() ? null : bidService.getLiveItemBidOrNull(item.getItemId());
        Auctioneer auctioneer = item.getAuctioneer();
        int highestBidAmount = liveBid == null ? -1 : liveBid.getHighestBid();
        Integer bidCount = liveBid == null ? 0 : liveBid.getHashMap().size();

        return new ItemListingSummaryResponse(
            item.getItemId(),
            item.getItemName(),
            item.getStartingPrice(),
            item.getDescription(),
            item.getCondition() == null ? null : item.getCondition().name(),
            auctioneer == null ? null : auctioneer.getAuctioneerId(),
            auctioneer == null ? null : auctioneer.getName(),
            liveBid == null ? null : liveBid.getHighestBidder(),
            highestBidAmount < 0 ? null : highestBidAmount,
            bidService.getActiveBidCount(item.getItemId()),
            item.getExpiresAt()
        );
    }
    
}
