package com.ccbid.biddingsite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ccbid.biddingsite.service.BidderService;
import com.ccbid.biddingsite.models.Bidder;

@RestController
@RequestMapping("/bidders")
public class BidderController {
    @Autowired 
    private BidderService service;

    @GetMapping("/all")
    public Iterable<Bidder> getBidders() {
       return service.getBidders();
    }

    @GetMapping("/{bidderId}")
    public Bidder getBidder(@PathVariable String bidderId) {
        return service.getBidder(bidderId);
    }

    @PostMapping("/add/{bidderId}/{name}")
    public String addBidder(@PathVariable String bidderId, @PathVariable String name) {
        return service.addBidder(bidderId, name);
    }
}
