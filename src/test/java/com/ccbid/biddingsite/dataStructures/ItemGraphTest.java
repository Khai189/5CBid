package com.ccbid.biddingsite.dataStructures;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemGraphTest {

    @Test
    void addEdgeStoresWeightsBidirectionally() {
        ItemGraph<String> graph = new ItemGraph<>();

        graph.addEdge("A", "B", 7, true);

        assertTrue(graph.hasEdge("A", "B"));
        assertTrue(graph.hasEdge("B", "A"));
        assertEquals(7, graph.getEdgeWeight("A", "B"));
        assertEquals(7, graph.getEdgeWeight("B", "A"));
    }

    @Test
    void dijkstraFindsShortestWeightedPaths() {
        ItemGraph<String> graph = new ItemGraph<>();
        graph.addEdge("A", "B", 4, true);
        graph.addEdge("A", "C", 10, true);
        graph.addEdge("B", "C", 3, true);
        graph.addEdge("C", "D", 2, true);

        Map<String, Integer> distances = graph.getShortestPaths("A");

        assertEquals(4, distances.get("B"));
        assertEquals(7, distances.get("C"));
        assertEquals(9, distances.get("D"));
    }
}
