package com.ccbid.biddingsite.service;

import org.springframework.stereotype.Service;
import com.ccbid.biddingsite.dataStructures.ItemGraph;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.repository.ItemRepo;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class RecService {

    @Autowired private ItemRepo itemRepo;
    @Autowired private BidService bidService;
    @Autowired private ItemService itemService;

    ItemGraph<BidItem> itemGraph = new ItemGraph<>();

    @PostConstruct
    @Transactional(readOnly = true)
    public void rehydrate() {
        for (BidItem item : itemRepo.findAll()) {
            itemGraph.addVertex(item);
        }
    }

    public List<BidItem> getRecommendations(String bidderId, String itemId, int totalRecs) {
        
        return null;
    }

    public List<BidItem> getFeed(String bidderId){
        return null;
    }


    

    
    
}
