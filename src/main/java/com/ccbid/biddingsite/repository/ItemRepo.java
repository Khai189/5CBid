package com.ccbid.biddingsite.repository;

import com.ccbid.biddingsite.models.BidItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepo extends JpaRepository<BidItem, String> {
    Iterable<BidItem> getItems();

    
}
