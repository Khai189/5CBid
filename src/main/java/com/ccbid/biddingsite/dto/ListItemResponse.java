package com.ccbid.biddingsite.dto;

public record ListItemResponse(
    String itemId,
    String itemName,
    String auctioneerId,
    String auctioneerName,
    String message
) {}
