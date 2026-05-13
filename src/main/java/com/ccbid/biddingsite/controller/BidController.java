package com.ccbid.biddingsite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbid.biddingsite.dataStructures.ItemBid;
import com.ccbid.biddingsite.dto.ListItemRequest;
import com.ccbid.biddingsite.dto.PlaceBidRequest;
import com.ccbid.biddingsite.service.BidService;


@RestController
@RequestMapping("/bid")
public class BidController {

    @Autowired
    private BidService service;

    @PostMapping("/list")
    public String listItem(@RequestBody ListItemRequest req) {
        ItemBid bid = service.addItem(
            req.itemId(),
            req.itemName(),
            req.startingPrice(),
            req.auctioneerId(),
            req.auctioneerName()
        );
        return "Listed " + bid.getItem().getItemId() + " (" + bid.getItem().getItemName()
            + ") by auctioneer " + bid.getAuctioneer().getAuctioneerId();
    }

    @PostMapping("/{itemId}")
    public String placeBid(@PathVariable String itemId, @RequestBody PlaceBidRequest req) {
        if (req.bidderId() == null || req.amount() == null) {
            throw new IllegalArgumentException("bidderId and amount are required");
        }
        return service.placeBid(itemId, req.bidderId(), req.amount());
    }

    @GetMapping("/{itemId}/highest")
    public String getHighest(@PathVariable String itemId) {
        String bidder = service.getHighestBidder(itemId);
        int amount = service.getHighestBid(itemId);
        if (bidder == null) {
            return "No bids on " + itemId;
        }
        return "Highest on " + itemId + ": " + bidder + " @ " + amount;
    }

    @DeleteMapping("/{itemId}/{bidderId}")
    public String removeBid(@PathVariable String itemId,
                            @PathVariable String bidderId) {
        return service.removeBid(itemId, bidderId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
