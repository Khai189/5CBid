package com.ccbid.biddingsite.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ccbid.biddingsite.repository.ItemRepo;
import com.ccbid.biddingsite.models.BidItem;

@Service
public class ItemService {
    @Autowired
    private ItemRepo repo;

    public Iterable<BidItem> getItems() {
        return repo.getItems();
    }


}
