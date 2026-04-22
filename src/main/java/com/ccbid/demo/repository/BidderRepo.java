package com.ccbid.demo.repository;

import org.springframework.stereotype.Repository;

@Repository
public interface BidderRepo {
    String getBidders();
    String getBidder(String bidderId);
    String addBidder(String bidderId, String name);
}
