package com.ccbid.biddingsite.models;

import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auctioneers")
public class Auctioneer {
    @Id
    private String auctioneerId;
    private String name;

    public String getAuctioneerId() {
        return auctioneerId;
    }

    public void setAuctioneerId(String auctioneerId) {
        this.auctioneerId = auctioneerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUpAuction(String itemId, String itemName, Integer startingPrice, Optional<String> description) {

    }
}
