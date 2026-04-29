package com.ccbid.biddingsite.repository;

import java.util.List;

import com.ccbid.biddingsite.models.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepo extends JpaRepository<Bid, Long> {
    List<Bid> findAllByActiveTrueOrderByCreatedAtAsc();
    List<Bid> findAllByItemIdAndBidderIdAndActiveTrue(String itemId, String bidderId);
}
