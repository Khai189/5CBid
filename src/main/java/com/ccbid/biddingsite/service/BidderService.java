package com.ccbid.biddingsite.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ccbid.biddingsite.models.Bidder;
import com.ccbid.biddingsite.repository.BidderRepo;

@Service
public class BidderService {
    @Autowired
    private BidderRepo repo;

    public Iterable<Bidder> getBidders() {
        return repo.findAll();
    }

    public Bidder getBidder(String bidderId) {
        return repo.findById(bidderId)
            .orElseThrow(() -> new IllegalStateException("Bidder " + bidderId + " not found"));
    }

    public String addBidder(String bidderId, String name) {
        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("bidderId is required");
        }
        if (repo.existsById(bidderId)) {
            throw new IllegalStateException("Bidder " + bidderId + " already exists");
        }
        Bidder b = new Bidder();
        b.setBidderId(bidderId);
        b.setName(name);
        repo.save(b);
        return "Added bidder " + bidderId;
    }

    

}
