package com.ccbid.biddingsite.dto;

import java.time.Instant;

public record BidHistorySummaryResponse(
    String itemId,
    String itemName,
    String description,
    Integer startingPrice,
    String auctioneerId,
    String auctioneerName,
    String highestBidderId,
    Integer highestBidAmount,
    Integer viewerBidAmount,
    Instant lastBidAt,
    Integer totalBidCount,
    boolean viewerOwnsListing
) {}
