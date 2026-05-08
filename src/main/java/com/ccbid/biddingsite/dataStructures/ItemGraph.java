package com.ccbid.biddingsite.dataStructures;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class ItemGraph<T> {
    private final Map<T, Map<T, Integer>> adjList = new HashMap<>();

    public void addVertex(T vertex) {
        adjList.putIfAbsent(vertex, new HashMap<>());
    }

    public void clear() {
        adjList.clear();
    }

    public void addEdge(T from, T to, int weight, boolean bidirectional) {
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
        if (!adjList.containsKey(from)) {
            addVertex(from);
        }
        if (!adjList.containsKey(to)) {
            addVertex(to);
        }
        adjList.get(from).merge(to, weight, Math::min);
        if (bidirectional) {
            adjList.get(to).merge(from, weight, Math::min);
        }
    }

    public int getVertexCount() {
        return adjList.size();
    }

    public int getEdgeCount() {
        int count = 0;
        for (Map<T, Integer> neighbors : adjList.values()) {
            count += neighbors.size();
        }
        return count;
    }

    public boolean hasVertex(T vertex) {
        return adjList.containsKey(vertex);
    }

    public boolean hasEdge(T from, T to) {
        return adjList.containsKey(from) && adjList.get(from).containsKey(to);
    }

    public Map<T, Integer> getNeighbors(T vertex) {
        return Collections.unmodifiableMap(adjList.getOrDefault(vertex, Collections.emptyMap()));
    }

    public Integer getEdgeWeight(T from, T to) {
        if (!adjList.containsKey(from)) {
            return null;
        }
        return adjList.get(from).get(to);
    }

    public Map<T, Integer> getShortestPaths(T source) {
        if (!adjList.containsKey(source)) {
            return Collections.emptyMap();
        }

        Map<T, Integer> distances = new HashMap<>();
        PriorityQueue<NodeDistance<T>> pq = new PriorityQueue<>(
            (a, b) -> Integer.compare(a.distance(), b.distance())
        );

        for (T vertex : adjList.keySet()) {
            distances.put(vertex, Integer.MAX_VALUE);
        }
        distances.put(source, 0);
        pq.offer(new NodeDistance<>(source, 0));

        while (!pq.isEmpty()) {
            NodeDistance<T> current = pq.poll();
            if (current.distance() > distances.get(current.node())) {
                continue;
            }

            for (Map.Entry<T, Integer> edge : adjList.get(current.node()).entrySet()) {
                int candidateDistance = current.distance() + edge.getValue();
                if (candidateDistance < distances.get(edge.getKey())) {
                    distances.put(edge.getKey(), candidateDistance);
                    pq.offer(new NodeDistance<>(edge.getKey(), candidateDistance));
                }
            }
        }

        distances.remove(source);
        distances.entrySet().removeIf(entry -> entry.getValue() == Integer.MAX_VALUE);
        return distances;
    }

    private record NodeDistance<T>(T node, int distance) {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<T, Map<T, Integer>> entry : adjList.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
