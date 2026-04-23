package com.ccbid.biddingsite.models;

import java.util.Optional;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Entity
public class BidItem {
    private String itemId;
    private String itemName;
    private Integer startingPrice;
    private Optional<String> description;

    public String getItemId() {
        return itemId;
    }
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public Integer getStartingPrice() {
        return startingPrice;
    }
    public void setStartingPrice(Integer startingPrice) {
        this.startingPrice = startingPrice;
    }
    public Optional<String> getDescription() {
        return description;
    }
    public void setDescription(Optional<String> description) {
        this.description = description;
    }
    
}
