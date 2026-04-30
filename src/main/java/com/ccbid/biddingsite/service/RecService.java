package com.ccbid.biddingsite.service;

import org.springframework.stereotype.Service;
import com.ccbid.biddingsite.dataStructures.ItemGraph;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.repository.ItemRepo;
import org.springframework.beans.factory.annotation.Autowired;
import com.ccbid.biddingsite.controller.RecController;

@Service
public class RecService {

    @Autowired private ItemRepo itemRepo;
    @Autowired private BidService bidService;
    @Autowired private ItemService itemService;
    @Autowired private RecController recController;

    ItemGraph<BidItem> itemGraph = new ItemGraph<>();

    

    
    
}
