package com.ccbid.biddingsite.repository;

import java.util.List;

import com.ccbid.biddingsite.models.BidItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepo extends JpaRepository<BidItem, String> {
    List<BidItem> findAllByArchivedFalseOrderByItemIdAsc();

    List<BidItem> findAllByAuctioneer_AuctioneerIdOrderByItemIdAsc(String auctioneerId);

    @Query("""
        select i
        from BidItem i
        where i.archived = false
        and (
            :query is null
            or trim(:query) = ''
            or lower(i.itemId) like lower(concat('%', :query, '%'))
            or lower(coalesce(i.itemName, '')) like lower(concat('%', :query, '%'))
            or lower(coalesce(i.description, '')) like lower(concat('%', :query, '%'))
            or lower(coalesce(i.auctioneer.auctioneerId, '')) like lower(concat('%', :query, '%'))
            or lower(coalesce(i.auctioneer.name, '')) like lower(concat('%', :query, '%'))
        )
        and (
            :auctioneerId is null
            or trim(:auctioneerId) = ''
            or lower(i.auctioneer.auctioneerId) = lower(:auctioneerId)
        )
        and (
            :minPrice is null
            or i.startingPrice is null
            or i.startingPrice >= :minPrice
        )
        and (
            :maxPrice is null
            or i.startingPrice is null
            or i.startingPrice <= :maxPrice
        )
        order by i.itemId asc
        """)
    List<BidItem> searchItems(
        @Param("query") String query,
        @Param("auctioneerId") String auctioneerId,
        @Param("minPrice") Integer minPrice,
        @Param("maxPrice") Integer maxPrice
    );
}
