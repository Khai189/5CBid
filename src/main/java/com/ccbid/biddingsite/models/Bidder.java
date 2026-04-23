package com.ccbid.biddingsite.models;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Bidder {
    public String bidderId;
    public String name;

    public void placeBid(BidItem item, Integer bidAmount) {
        
    }
    
}
