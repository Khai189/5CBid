package com.ccbid.biddingsite.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.repository.ItemRepo;

@Service
public class ItemService {
    @Autowired
    private ItemRepo repo;

    public Iterable<BidItem> getItems() {
        return repo.findAll();
    }

    public BidItem getItem(String itemId) {
        return repo.findById(itemId)
            .orElseThrow(() -> new IllegalStateException("Item " + itemId + " not found"));
    }
}
