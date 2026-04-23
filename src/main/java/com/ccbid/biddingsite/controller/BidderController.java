package com.ccbid.biddingsite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbid.biddingsite.service.BidderService;
import com.ccbid.biddingsite.models.Bidder;

@RestController
@RequestMapping("/bidders")
public class BidderController {
    @Autowired 
    private BidderService service;

    @RequestMapping("/all")
    public Iterable<Bidder> getBidders() {
       return service.getBidders();
    }
    @RequestMapping("{bidderId}")
    public Bidder getBidder(@PathVariable String bidderId) {
        return service.getBidder(bidderId);
    }

    @RequestMapping("/add/{bidderId}/{name}")
    public String addBidder(@PathVariable String bidderId, @PathVariable String name) {
        return service.addBidder(bidderId, name);
    }
}
