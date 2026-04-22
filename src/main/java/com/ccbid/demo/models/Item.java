package com.ccbid.demo.models;

import java.util.Optional;
public class Item {
    private String itemId;
    private String itemName;
    private Integer startingPrice;
    private Optional<String> description;

    public Item(String itemId, String itemName, Integer startingPrice, Optional<String> description) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.startingPrice = startingPrice;
        this.description = description;
    }
    
}
