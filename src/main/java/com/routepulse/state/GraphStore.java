package com.routepulse.state;

import com.routepulse.domain.Edge;
import com.routepulse.domain.NodeId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mutable adjacency-list store for the delivery network graph.
 * Holds all nodes and directed, weighted edges. This is the authoritative
 * graph structure used by Dijkstra and all graph-reading operations.
 *
 * <p><strong>Mutation contract:</strong> All write operations on this store
 * (addEdge, updateEdgeWeight, removeEdge) must be called exclusively by
 * {@code MutationApplier}. No algorithm or service layer may write to this store directly.
 */
@Component
public class GraphStore {

    /** The adjacency list mapping each NodeId to its outgoing directed edges. */
    private final Map<NodeId, List<Edge>> adjacencyList = new HashMap<>();

    /**
     * Registers a node in the graph with an empty adjacency list.
     * Safe to call if the node already exists (idempotent).
     *
     * @param nodeId the node to register
     */
    public void addNode(NodeId nodeId) {
        adjacencyList.putIfAbsent(nodeId, new ArrayList<>());
    }

    /**
     * Adds a directed edge to the graph.
     * Both the source and destination nodes must already be registered.
     *
     * @param edge the directed weighted edge to add
     * @throws IllegalStateException if the source node is not registered
     */
    public void addEdge(Edge edge) {
        List<Edge> edges = requireNode(edge.from());
        edges.add(edge);
    }

    /**
     * Updates the weight of an existing directed edge.
     * If no edge from {@code from} to {@code to} exists, this is a no-op.
     *
     * @param from      source node of the edge to update
     * @param to        destination node of the edge to update
     * @param newWeight the new edge weight (must satisfy Edge constraints)
     */
    public void updateEdgeWeight(NodeId from, NodeId to, int newWeight) {
        List<Edge> edges = getEdgesOrEmpty(from);
        edges.replaceAll(edge -> edge.to().equals(to) ? edge.withWeight(newWeight) : edge);
    }

    /**
     * Removes the directed edge from {@code from} to {@code to} from the graph.
     * If no such edge exists, this is a no-op.
     *
     * @param from source node of the edge to remove
     * @param to   destination node of the edge to remove
     */
    public void removeEdge(NodeId from, NodeId to) {
        getEdgesOrEmpty(from).removeIf(edge -> edge.to().equals(to));
    }

    /**
     * Returns an unmodifiable view of all outgoing edges from the given node.
     * Returns an empty list if the node is not registered.
     *
     * @param nodeId the source node
     * @return unmodifiable list of outgoing directed edges
     */
    public List<Edge> getNeighbors(NodeId nodeId) {
        return Collections.unmodifiableList(
                adjacencyList.getOrDefault(nodeId, Collections.emptyList()));
    }

    /**
     * Returns the specific edge from {@code from} to {@code to}, if it exists.
     *
     * @param from source node
     * @param to   destination node
     * @return Optional containing the edge, or empty if no such edge exists
     */
    public Optional<Edge> findEdge(NodeId from, NodeId to) {
        return getEdgesOrEmpty(from).stream()
                .filter(edge -> edge.to().equals(to))
                .findFirst();
    }

    /**
     * Returns an unmodifiable view of all registered node IDs in the graph.
     *
     * @return set of all node identifiers
     */
    public java.util.Set<NodeId> getAllNodes() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }

    /** Returns the total number of registered nodes in the graph. */
    public int nodeCount() {
        return adjacencyList.size();
    }

    /**
     * Removes all nodes and edges — used for test teardown and simulation reset.
     */
    public void clear() {
        adjacencyList.clear();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private List<Edge> requireNode(NodeId nodeId) {
        List<Edge> edges = adjacencyList.get(nodeId);
        if (edges == null) {
            throw new IllegalStateException(
                    "Node not registered in GraphStore: " + nodeId);
        }
        return edges;
    }

    private List<Edge> getEdgesOrEmpty(NodeId nodeId) {
        return adjacencyList.getOrDefault(nodeId, new ArrayList<>());
    }
}
