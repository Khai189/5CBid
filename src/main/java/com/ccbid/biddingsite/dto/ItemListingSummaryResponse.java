package com.ccbid.biddingsite.dto;

import java.time.Instant;

public record ItemListingSummaryResponse(
    String itemId,
    String itemName,
    Double startingPrice,
    String description,
    String condition,
    String auctioneerId,
    String auctioneerName,
    String highestBidderId,
    Integer highestBidAmount,
    Integer bidCount,
    Instant expiresAt
) {
}
