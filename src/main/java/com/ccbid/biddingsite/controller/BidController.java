package com.ccbid.biddingsite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
import com.ccbid.biddingsite.models.UserAccount;
import com.ccbid.biddingsite.service.AuthService;
import com.ccbid.biddingsite.service.BidService;


@RestController
@RequestMapping("/bid")
public class BidController {

    @Autowired
    private BidService service;

    @Autowired
    private AuthService authService;

    @PostMapping("/list")
    public String listItem(@AuthenticationPrincipal UserDetails principal, @RequestBody ListItemRequest req) {
        UserAccount account = authService.getRequiredAccount(principal.getUsername());
        ItemBid bid = service.addItem(
            req.itemId(),
            req.itemName(),
            req.startingPrice(),
            account.getUsername(),
            account.getDisplayName()
        );
        return "Listed " + bid.getItem().getItemId() + " (" + bid.getItem().getItemName()
            + ") by auctioneer " + bid.getAuctioneer().getAuctioneerId();
    }

    @PostMapping("/{itemId}")
    public String placeBid(@AuthenticationPrincipal UserDetails principal,
                           @PathVariable String itemId,
                           @RequestBody PlaceBidRequest req) {
        if (req.amount() == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (req.bidderId() != null && !req.bidderId().isBlank() && !req.bidderId().equals(principal.getUsername())) {
            throw new IllegalArgumentException("Authenticated user does not match bidderId");
        }
        return service.placeBid(itemId, principal.getUsername(), req.amount());
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

    @DeleteMapping("/{itemId}")
    public String removeOwnBid(@AuthenticationPrincipal UserDetails principal,
                               @PathVariable String itemId) {
        return service.removeBid(itemId, principal.getUsername());
    }

    @DeleteMapping("/{itemId}/{bidderId}")
    public String removeBid(@AuthenticationPrincipal UserDetails principal,
                            @PathVariable String itemId,
                            @PathVariable String bidderId) {
        if (!isAdmin(principal) && !principal.getUsername().equals(bidderId)) {
            throw new IllegalArgumentException("Authenticated user does not match bidderId");
        }
        String effectiveBidderId = isAdmin(principal) ? bidderId : principal.getUsername();
        return service.removeBid(itemId, effectiveBidderId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    private boolean isAdmin(UserDetails principal) {
        return principal.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
