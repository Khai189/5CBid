package com.ccbid.biddingsite.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Entity
/**
 * Represents a participant bidding in the auction system 
 * 
 * Bidders can place bids on various items; tracks identity details 
 * for bidding actions
 */
public class Bidder {
    @Id
    private String bidderId;
    private String name;

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void placeBid(BidItem item, Integer bidAmount) {

    }
}
