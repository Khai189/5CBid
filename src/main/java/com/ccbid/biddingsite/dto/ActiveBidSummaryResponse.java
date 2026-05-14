package com.ccbid.biddingsite.dto;

public record ActiveBidSummaryResponse(
    String itemId,
    String itemName,
    String description,
    Integer startingPrice,
    String auctioneerId,
    String auctioneerName,
    String highestBidderId,
    Integer highestBidAmount,
    Integer viewerBidAmount,
    boolean viewerIsHighestBidder,
    boolean viewerOwnsListing
) {}
