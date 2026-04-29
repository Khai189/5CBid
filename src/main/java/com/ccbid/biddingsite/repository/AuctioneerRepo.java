package com.ccbid.biddingsite.repository;

import com.ccbid.biddingsite.models.Auctioneer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctioneerRepo extends JpaRepository<Auctioneer, String> {
}
