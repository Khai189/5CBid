package com.ccbid.biddingsite.dto;

public record ListItemRequest(
    String itemId,
    String itemName,
    Double startingPrice,
    String description,
    String condition,
    String auctioneerId,
    String auctioneerName
) {}
