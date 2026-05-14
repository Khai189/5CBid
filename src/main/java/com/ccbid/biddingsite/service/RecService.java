package com.ccbid.biddingsite.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import com.ccbid.biddingsite.dataStructures.ItemGraph;
import com.ccbid.biddingsite.models.Bid;
import com.ccbid.biddingsite.models.BidItem;
import com.ccbid.biddingsite.repository.BidRepo;
import com.ccbid.biddingsite.repository.ItemRepo;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class RecService {

    @Autowired private ItemRepo itemRepo;
    @Autowired private BidRepo bidRepo;

    private final ItemGraph<String> itemGraph = new ItemGraph<>();
    private final Map<String, BidItem> itemsById = new HashMap<>();

    @PostConstruct
    @Transactional(readOnly = true)
    public void rehydrate() {
        rebuildGraph();
    }

    public List<BidItem> getRecommendations(String bidderId, String itemId, int totalRecs) {
        rebuildGraph();
        if (totalRecs <= 0) {
            return List.of();
        }
        if (!itemsById.containsKey(itemId)) {
            throw new IllegalStateException("Item " + itemId + " not found");
        }

        Set<String> excluded = getBidderHistory(bidderId);
        excluded.add(itemId);
        return rankItemsByDistance(itemId, excluded, totalRecs);
    }

    public List<BidItem> getFeed(String bidderId) {
        rebuildGraph();
        Set<String> bidderHistory = getBidderHistory(bidderId);
        if (bidderHistory.isEmpty()) {
            return itemRepo.findAll().stream().limit(10).toList();
        }

        Map<String, Integer> bestDistances = new HashMap<>();
        for (String itemId : bidderHistory) {
            for (Map.Entry<String, Integer> path : itemGraph.getShortestPaths(itemId).entrySet()) {
                if (bidderHistory.contains(path.getKey())) {
                    continue;
                }
                bestDistances.merge(path.getKey(), path.getValue(), Math::min);
            }
        }

        return bestDistances.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(10)
            .map(Map.Entry::getKey)
            .map(itemsById::get)
            .filter(item -> item != null)
            .toList();
    }

    private void rebuildGraph() {
        itemGraph.clear();
        itemsById.clear();
        Map<String, Set<String>> itemsByBidder = new HashMap<>();

        for (BidItem item : itemRepo.findAllByArchivedFalseOrderByItemIdAsc()) {
            itemsById.put(item.getItemId(), item);
            itemGraph.addVertex(item.getItemId());
        }

        for (Bid bid : bidRepo.findAllByActiveTrueOrderByCreatedAtAsc()) {
            itemsByBidder
                .computeIfAbsent(bid.getBidderId(), ignored -> new LinkedHashSet<>())
                .add(bid.getItemId());
        }

        for (Set<String> bidderItems : itemsByBidder.values()) {
            addCoBidEdges(new ArrayList<>(bidderItems));
        }
    }

    // private void addCoBidEdges(List<String> itemIds) {
    //     for (int i = 0; i < itemIds.size(); i++) {
    //         for (int j = i + 1; j < itemIds.size(); j++) {
    //             String from = itemIds.get(i);
    //             String to = itemIds.get(j);
    //             Integer existingWeight = itemGraph.getEdgeWeight(from, to);
    //             int updatedWeight = existingWeight == null ? 100 : Math.max(1, existingWeight - 10);
    //             itemGraph.addEdge(from, to, updatedWeight, true);
    //         }
    //     }
    // }
    private void addCoBidEdges(List<String> itemIds) {
        for (int i = 0; i < itemIds.size(); i++) {
            for (int j = i + 1; j < itemIds.size(); j++) {
                String idA = itemIds.get(i);
                String idB = itemIds.get(j); 

                BidItem itemA = itemsById.get(idA); 
                BidItem itemB = itemsById.get(idB);

                if (itemA != null && itemB != null) {
                    // Get the lower difference between these two prices 
                    int priceA = itemA.getStartingPrice(); 
                    int priceB = itemB.getStartingPrice();  

                    // Ensure that we don't have a 0-weight edge
                    int weight = Math.abs(priceA - priceB) + 1; 

                    itemGraph.addEdge(idA, idB, weight, true);
                }

            }
         }
    }

    private Set<String> getBidderHistory(String bidderId) {
        Set<String> history = new LinkedHashSet<>();
        for (Bid bid : bidRepo.findAllByBidderIdAndActiveTrueOrderByCreatedAtAsc(bidderId)) {
            history.add(bid.getItemId());
        }
        return history;
    }

    private List<BidItem> rankItemsByDistance(String sourceItemId, Set<String> excludedItemIds, int limit) {
        return itemGraph.getShortestPaths(sourceItemId).entrySet().stream()
            .filter(entry -> !excludedItemIds.contains(entry.getKey()))
            .sorted(Map.Entry.comparingByValue())
            .limit(limit)
            .map(Map.Entry::getKey)
            .map(itemsById::get)
            .filter(item -> item != null)
            .toList();
    }
}
