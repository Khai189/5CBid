package com.ccbid.biddingsite.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ccbid.biddingsite.repository.BidderRepo;


@Service
public class BidderService {
    @Autowired
    private BidderRepo repo;
    
    public String getBidders() {
        return repo.getBidders();
    }

    public String getBidder(String bidderId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBidder'");
    }

    public String addBidder(String bidderId, String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addBidder'");
    }
    
}
