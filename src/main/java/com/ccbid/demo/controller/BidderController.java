package com.ccbid.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ccbid.demo.service.BidderService;

@RestController
@RequestMapping("/bidders")
public class BidderController {
    @Autowired 
    private BidderService service;

    @RequestMapping("/all")
    public String getBidders() {
       return service.getBidders();
    }
    @RequestMapping("{bidderId}")
    public String getBidder(@PathVariable String bidderId) {
        return service.getBidder(bidderId);
    }

    @RequestMapping("/add/{bidderId}/{name}")
    public String addBidder(@PathVariable String bidderId, @PathVariable String name) {
        return service.addBidder(bidderId, name);
    }
}
