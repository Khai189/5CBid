package com.ccbid.biddingsite.repository;

import com.ccbid.biddingsite.models.Bidder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidderRepo extends JpaRepository<Bidder, String> {
    Iterable<Bidder> getBidders();
    Bidder getBidder(String bidderId);
    Bidder addBidder(String bidderId, String name);
    Bidder getMaxBidder(String itemId);
}
