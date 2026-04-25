package com.ccbid.biddingsite.dto;

public record ListItemRequest(
    String itemId,
    String itemName,
    Integer startingPrice,
    String auctioneerId,
    String auctioneerName
) {}
