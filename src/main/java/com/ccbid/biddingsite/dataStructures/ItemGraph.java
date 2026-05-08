package com.ccbid.biddingsite.dataStructures;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ItemGraph<T> {
    private Map<T, List<T>> adjList = new HashMap<>();

    public void addVertex(T vertex) {
        adjList.putIfAbsent(vertex, new LinkedList<>());
    }

    public void addEdge(T from, T to, int weight, boolean bidirectional) {
        if(!adjList.containsKey(from)){
            addVertex(from);
        }
        if(!adjList.containsKey(to)){
            addVertex(to);
        }
        adjList.putIfAbsent(from, new LinkedList<>());
        adjList.putIfAbsent(to, new LinkedList<>());
        adjList.get(from).add(to);
        if (bidirectional) {
            adjList.get(to).add(from);
        }

    }

    public int getVertexCount() {
        return adjList.size();
    }
    public int getEdgeCount() {
        int count = 0;
        for (List<T> neighbors : adjList.values()) {
            count += neighbors.size();
        }
        return count;
    }

    public boolean hasVertex(T vertex) {
        return adjList.containsKey(vertex);
    }

    public boolean hasEdge(T from, T to) {
        return adjList.get(from).contains(to);
    }

    public List<T> getNeighbors(T vertex) {
        return adjList.get(vertex);
    }

    public Map<T, List<T>> getShortestPaths(){
        return null;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<T, List<T>> entry : adjList.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
