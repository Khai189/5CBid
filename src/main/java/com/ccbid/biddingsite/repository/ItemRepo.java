package com.ccbid.biddingsite.repository;

import java.util.Optional;
import java.util.List;

import com.ccbid.biddingsite.models.BidItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface ItemRepo extends JpaRepository<BidItem, String> {
    List<BidItem> findAllByArchivedFalseOrderByItemIdAsc();

    List<BidItem> findAllByAuctioneer_AuctioneerIdOrderByItemIdAsc(String auctioneerId);

    boolean existsByItemIdStartingWith(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BidItem> findByItemId(String itemId);
}
