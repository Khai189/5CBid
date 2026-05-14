package com.ccbid.biddingsite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccbid.biddingsite.dto.ItemListingSummaryResponse;
import com.ccbid.biddingsite.service.ItemService;

/**
 * This is the controller for handling all the item requests
 * Important Methods:
 * Get Items: Returns a list of all the items
 * Get Item: Returns a specific item based on its ID
 * Exception Handlers: Handles exceptions for conflicts (Codex helped generate this, prompt is menntionned in other controllers)
 */

@RestController
@RequestMapping("/items")
public class ItemController {

    @Autowired
    private ItemService service;

    @GetMapping("/all")
    public Iterable<ItemListingSummaryResponse> getItems(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) String auctioneerId,
        @RequestParam(required = false) Double minPrice,
        @RequestParam(required = false) Double maxPrice,
        @RequestParam(required = false) String condition
    ) {
        return service.getItems(query, auctioneerId, minPrice, maxPrice, condition);
    }

    @GetMapping("/{itemId}")
    public ItemListingSummaryResponse getItem(@PathVariable String itemId) {
        return service.getItem(itemId);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
