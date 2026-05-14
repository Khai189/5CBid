package com.ccbid.biddingsite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.service.RecService;


/**
 * This is the controller for handling all the recommendation requests
 * Important Methods:
 * Get Recs: Returns a list of recommended items for a bidder based on their bidding history
 * Get Feed: Returns a list of recommended items for a bidder based on their bidding history and the current active items, this is more of a "feed" of items that they might be interested in
 * Exception Handlers: Handles exceptions for conflicts (Codex helped generate this, prompt is menntionned in other controllers)
 */
@RestController
public class RecController {

    @Autowired private RecService service;

    @GetMapping("/rec/{bidderId}/{itemId}/{totalRecs}")
    public List<BidItem> getRecs(@PathVariable String bidderId,
                                 @PathVariable String itemId,
                                 @PathVariable int totalRecs) {
        return service.getRecommendations(bidderId, itemId, totalRecs);
    }

    @GetMapping("/feed/{bidderId}")
    public List<BidItem> getFeed(@PathVariable String bidderId) {
        return service.getFeed(bidderId);
    }
}
