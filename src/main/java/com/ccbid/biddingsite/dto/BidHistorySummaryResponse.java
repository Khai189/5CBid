package com.ccbid.biddingsite.dto;

import java.time.Instant;

public record BidHistorySummaryResponse(
    String itemId,
    String itemName,
    String description,
    Double startingPrice,
    String condition,
    String auctioneerId,
    String auctioneerName,
    String highestBidderId,
    Integer highestBidAmount,
    Integer viewerBidAmount,
    Instant lastBidAt,
    Integer totalBidCount,
    boolean viewerOwnsListing
) {}
