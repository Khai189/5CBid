package com.ccbid.biddingsite.dto;

public record ListItemRequest(
    String itemId,
    String itemName,
    Integer startingPrice,
    String description,
    String auctioneerId,
    String auctioneerName
) {}
