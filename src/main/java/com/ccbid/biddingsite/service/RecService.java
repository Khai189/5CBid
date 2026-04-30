package com.ccbid.biddingsite.service;

import org.springframework.stereotype.Service;
import com.ccbid.biddingsite.dataStructures.ItemGraph;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.repository.ItemRepo;
import org.springframework.beans.factory.annotation.Autowired;


@Service
public class RecService {

    @Autowired
    private ItemRepo itemRepo;

    @Autowired
    private BidService bidService;



    ItemGraph<BidItem> itemGraph = new ItemGraph<>();

    

    
    
}
