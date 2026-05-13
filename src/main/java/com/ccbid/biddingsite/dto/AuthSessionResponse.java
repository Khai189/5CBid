package com.ccbid.biddingsite.dto;

public record AuthSessionResponse(
    String accessToken,
    String tokenType,
    AuthResponse user
) {}
