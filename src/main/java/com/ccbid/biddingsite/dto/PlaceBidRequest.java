package com.ccbid.biddingsite.dto;

public record PlaceBidRequest(
    String bidderId,
    Integer amount
) {}
