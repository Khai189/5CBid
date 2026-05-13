package com.ccbid.biddingsite.dto;

public record RegisterRequest(
    String username,
    String email,
    String password,
    String displayName,
    String role
) {}
