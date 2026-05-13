package com.ccbid.biddingsite.dto;

public record AuthLoginRequest(
    String username,
    String password
) {}
