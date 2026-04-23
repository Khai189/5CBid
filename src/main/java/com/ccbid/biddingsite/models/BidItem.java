package com.ccbid.biddingsite.models;

import java.util.Optional;
public class BidItem {
    private String itemId;
    private String itemName;
    private Integer startingPrice;
    private Optional<String> description;

    public BidItem(String itemId, String itemName, Integer startingPrice, Optional<String> description) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.description = description;
    }
    
}
