package com.ccbid.biddingsite.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.repository.ItemRepo;

@Service
public class ItemService {
    @Autowired
    private ItemRepo repo;

    public List<BidItem> getItems(String query, String auctioneerId, Integer minPrice, Integer maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }
        return repo.searchItems(query, auctioneerId, minPrice, maxPrice);
    }

    public BidItem getItem(String itemId) {
        return repo.findById(itemId)
            .orElseThrow(() -> new IllegalStateException("Item " + itemId + " not found"));
    }

    public BidItem updateItem(String itemId, String itemName, Integer startingPrice){
        BidItem item = getItem(itemId);
        item.setItemName(itemName);
        item.setStartingPrice(startingPrice);
        return repo.save(item);
    }

    public HttpStatus addItem(String itemId, String itemName, Integer startingPrice){
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is required");
        }
        if (repo.existsById(itemId)) {
            throw new IllegalStateException("Item " + itemId + " already exists");
        }
        BidItem item = new BidItem();
        item.setItemId(itemId);
        item.setItemName(itemName);
        item.setStartingPrice(startingPrice);
        repo.save(item);
        return HttpStatus.CREATED;
    }

    public HttpStatus deleteItem(String itemId){
        if (!repo.existsById(itemId)) {
            throw new IllegalStateException("Item " + itemId + " does not exist");
        }
        repo.deleteById(itemId);
        return HttpStatus.OK;
    }
    
}
