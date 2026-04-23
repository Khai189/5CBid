package com.ccbid.biddingsite.models;

import java.util.Optional;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Auctioneer {
    public String auctioneerId;
    public String name;

    public void setUpAuction(String itemId, String itemName, Integer startingPrice, Optional<String> description) {
        
    }
    
}
