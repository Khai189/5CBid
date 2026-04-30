package com.ccbid.biddingsite.service;

import org.springframework.stereotype.Service;
import com.ccbid.biddingsite.dataStructures.ItemGraph;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.repository.ItemRepo;

import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import com.ccbid.biddingsite.controller.RecController;

@Service
public class RecService {

    @Autowired private ItemRepo itemRepo;
    @Autowired private BidService bidService;
    @Autowired private ItemService itemService;
    @Autowired private RecController recController;

    ItemGraph<BidItem> itemGraph = new ItemGraph<>();

    public List<BidItem> getRecommendations(String itemId, int totalRecs) {
        if (totalRecs <= 0) {
            return Collections.emptyList();
        }

        BidItem currentItem = itemService.getItem(itemId);
        String currentAuctioneerId = currentItem.getAuctioneer() == null
            ? null
            : currentItem.getAuctioneer().getAuctioneerId();
        int currentStartingPrice = currentItem.getStartingPrice() == null
            ? 0
            : currentItem.getStartingPrice();

        return itemRepo.findAll().stream()
            .filter(item -> !itemId.equals(item.getItemId()))
            .sorted(Comparator
                .comparing((BidItem item) -> {
                    String auctioneerId = item.getAuctioneer() == null
                        ? null
                        : item.getAuctioneer().getAuctioneerId();
                    return !java.util.Objects.equals(currentAuctioneerId, auctioneerId);
                })
                .thenComparingInt(item -> Math.abs((item.getStartingPrice() == null ? 0 : item.getStartingPrice()) - currentStartingPrice))
                .thenComparing(BidItem::getItemId))
            .limit(totalRecs)
            .collect(Collectors.toList());
    }

    

    
    
}
