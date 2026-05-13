package com.ccbid.biddingsite.dto;

public record AuthResponse(
    String username,
    String email,
    String displayName,
    String role,
    String profileId
) {}
