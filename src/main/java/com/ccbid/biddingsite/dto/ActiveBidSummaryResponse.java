package com.ccbid.biddingsite.dto;

public record ActiveBidSummaryResponse(
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
    boolean viewerIsHighestBidder,
    boolean viewerOwnsListing
) {}
