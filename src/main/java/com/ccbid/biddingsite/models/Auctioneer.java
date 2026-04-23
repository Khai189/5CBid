package com.ccbid.biddingsite.models;

import java.util.Optional;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auctioneers")
public class Auctioneer {
    @Id
    public String auctioneerId;
    public String name;

    public void setUpAuction(String itemId, String itemName, Integer startingPrice, Optional<String> description) {
        
    }
    
}
