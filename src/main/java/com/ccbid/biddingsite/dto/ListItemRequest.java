package com.ccbid.biddingsite.dto;

public record ListItemRequest(
    String itemName,
    Double startingPrice,
    String description,
    String condition,
    Integer durationAmount,
    String durationUnit
) {}
