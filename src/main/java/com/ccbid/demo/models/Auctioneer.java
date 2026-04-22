package com.ccbid.demo.models;

import java.util.Optional;

public class Auctioneer {
    public String auctioneerId;
    public String name;

    public Auctioneer(String auctioneerId, String name) {
        this.auctioneerId = auctioneerId;
        this.name = name;
    }

    public void setUpAuction(String itemId, String itemName, Integer startingPrice, Optional<String> description) {
        
    }
    
}
