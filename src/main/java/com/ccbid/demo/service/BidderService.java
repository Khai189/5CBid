package com.ccbid.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BidderService {
    @Autowired
    private BidderRepository repo;
    
    public String getBidders() {
        return repo.getBidders();
    }
    
}
