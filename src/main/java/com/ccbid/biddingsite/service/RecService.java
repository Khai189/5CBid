package com.ccbid.biddingsite.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
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
    @Autowired private BidService bidService;

    private final ItemGraph<String> itemGraph = new ItemGraph<>();
    private final Map<String, BidItem> itemsById = new HashMap<>();

    @PostConstruct
    @Transactional(readOnly = true)
    public void rehydrate() {
        bidService.expireStaleListings();
        rebuildGraph();
    }

    public List<BidItem> getRecommendations(String bidderId, String itemId, int totalRecs) {
        bidService.expireStaleListings();
        rebuildGraph();
        if (totalRecs <= 0) {
            return List.of();
        }
        if (!itemsById.containsKey(itemId)) {
            throw new IllegalStateException("Item " + itemId + " not found");
        }

        Set<String> excluded = getBidderHistory(bidderId);
        excluded.add(itemId);
        return rankItemsByDistance(itemId, excluded, totalRecs, getPreferredPriceCeiling(bidderId));
    }

    public List<BidItem> getFeed(String bidderId) {
        bidService.expireStaleListings();
        rebuildGraph();
        Set<String> bidderHistory = getBidderHistory(bidderId);
        if (bidderHistory.isEmpty()) {
            return itemRepo.findAllByArchivedFalseOrderByItemIdAsc().stream().limit(10).toList();
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

        OptionalDouble preferredPriceCeiling = getPreferredPriceCeiling(bidderId);
        return bestDistances.entrySet().stream()
            .map(entry -> new CandidateScore(itemsById.get(entry.getKey()), entry.getValue(),
                getBudgetPenalty(itemsById.get(entry.getKey()), preferredPriceCeiling)))
            .filter(candidate -> candidate.item() != null)
            .sorted(candidateComparator())
            .limit(10)
            .map(CandidateScore::item)
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

    private void addCoBidEdges(List<String> itemIds) {
        for (int i = 0; i < itemIds.size(); i++) {
            for (int j = i + 1; j < itemIds.size(); j++) {
                String from = itemIds.get(i);
                String to = itemIds.get(j);
                Integer existingWeight = itemGraph.getEdgeWeight(from, to);
                int updatedWeight = existingWeight == null ? 100 : Math.max(1, existingWeight - 10);
                itemGraph.addEdge(from, to, updatedWeight, true);
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

    private List<BidItem> rankItemsByDistance(String sourceItemId, Set<String> excludedItemIds, int limit,
                                              OptionalDouble preferredPriceCeiling) {
        return itemGraph.getShortestPaths(sourceItemId).entrySet().stream()
            .filter(entry -> !excludedItemIds.contains(entry.getKey()))
            .map(entry -> new CandidateScore(itemsById.get(entry.getKey()), entry.getValue(),
                getBudgetPenalty(itemsById.get(entry.getKey()), preferredPriceCeiling)))
            .filter(candidate -> candidate.item() != null)
            .sorted(candidateComparator())
            .limit(limit)
            .map(CandidateScore::item)
            .toList();
    }

    private OptionalDouble getPreferredPriceCeiling(String bidderId) {
        return bidRepo.findAllByBidderIdOrderByCreatedAtAsc(bidderId).stream()
            .map(Bid::getAmount)
            .filter(amount -> amount != null && amount > 0)
            .mapToDouble(Integer::doubleValue)
            .max();
    }

    private double getBudgetPenalty(BidItem item, OptionalDouble preferredPriceCeiling) {
        if (item == null || item.getStartingPrice() == null) {
            return Double.MAX_VALUE;
        }
        if (preferredPriceCeiling.isEmpty()) {
            return 0d;
        }
        return Math.max(0d, item.getStartingPrice() - preferredPriceCeiling.getAsDouble());
    }

    private Comparator<CandidateScore> candidateComparator() {
        return Comparator.comparingDouble(CandidateScore::budgetPenalty)
            .thenComparingInt(CandidateScore::distance)
            .thenComparing(candidate -> candidate.item().getStartingPrice(), Comparator.nullsLast(Double::compareTo))
            .thenComparing(candidate -> candidate.item().getItemId());
    }

    private record CandidateScore(BidItem item, int distance, double budgetPenalty) {
    }
}
