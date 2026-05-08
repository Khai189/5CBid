package com.ccbid.biddingsite.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

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
