package com.ccbid.biddingsite.repository;

import com.ccbid.biddingsite.models.Bidder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidderRepo extends JpaRepository<Bidder, String> {
}
