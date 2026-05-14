package com.ccbid.biddingsite.dto;

import java.time.Instant;

public record ListItemResponse(
    String itemId,
    String itemName,
    String auctioneerId,
    String auctioneerName,
    Instant expiresAt,
    String message
) {}
