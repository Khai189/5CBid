package com.ccbid.biddingsite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.ArrayList;
import java.util.List;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.service.RecService;

@RestController
public class RecController {

    @Autowired private RecService service;

    @GetMapping("/rec/{itemId}/{totalRecs}")
    public List<BidItem> getRecs(@RequestParam String itemId, @RequestParam int totalRecs) {
        return service.getRecommendations(itemId, totalRecs);
    }

    @GetMapping("/feed/{bidderId}")
    public List<BidItem> getFeed(@RequestParam String bidderId){
        return service.getFeed(bidderId);
    }


    
    
}
