package com.ccbid.biddingsite.repository;

import java.util.List;

import com.ccbid.biddingsite.models.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

// For Spring Data JPA, most methods are automatically implemented. We can also use extremely descriptive names and the Hibernate ORM just kinda automatically knows what query we need. Kinda magic
public interface BidRepo extends JpaRepository<Bid, Long> {
    List<Bid> findAllByActiveTrueOrderByCreatedAtAsc();
    List<Bid> findAllByItemIdAndBidderIdAndActiveTrue(String itemId, String bidderId);
    List<Bid> findAllByBidderIdOrderByCreatedAtAsc(String bidderId);
    List<Bid> findAllByBidderIdAndActiveTrueOrderByCreatedAtAsc(String bidderId);
    List<Bid> findAllByBidderIdAndActiveFalseOrderByCreatedAtAsc(String bidderId);
    List<Bid> findAllByItemIdOrderByCreatedAtAsc(String itemId);
}
