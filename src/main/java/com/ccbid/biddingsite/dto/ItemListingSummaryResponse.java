package com.ccbid.biddingsite.dto;

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
    Integer bidCount
) {
}
