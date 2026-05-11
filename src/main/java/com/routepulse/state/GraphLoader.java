package com.routepulse.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routepulse.algorithm.dijkstra.DijkstraInput;
import com.routepulse.algorithm.dijkstra.DijkstraModule;
import com.routepulse.algorithm.dijkstra.DijkstraOutput;
import com.routepulse.domain.Edge;
import com.routepulse.domain.Node;
import com.routepulse.domain.NodeId;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the demo graph from {@code classpath:graphs/demo_graph.json} at application startup
 * and pre-populates the {@link GraphStore} and {@link DistanceMatrix}.
 *
 * <p>After loading all nodes and edges, runs Dijkstra from every node to fully initialize
 * the 30×30 distance matrix. This is the only place full-matrix initialization occurs.
 *
 * <p>Expected JSON format:
 * <pre>{@code
 * {
 *   "nodes": [{ "id": 0, "label": "Depot", "x": 400, "y": 300 }, ...],
 *   "edges": [{ "from": 0, "to": 1, "weight": 5 }, ...]
 * }
 * }</pre>
 */
@Component
public class GraphLoader {

    private static final Logger log = LoggerFactory.getLogger(GraphLoader.class);
    private static final String DEMO_GRAPH_PATH = "graphs/demo_graph.json";

    private final GraphStore graphStore;
    private final DistanceMatrix distanceMatrix;
    private final DijkstraModule dijkstraModule;
    private final ObjectMapper objectMapper;

    /** Stores all loaded Node objects for downstream use (e.g., API responses). */
    private final List<Node> loadedNodes = new ArrayList<>();

    public GraphLoader(GraphStore graphStore,
                       DistanceMatrix distanceMatrix,
                       DijkstraModule dijkstraModule,
                       ObjectMapper objectMapper) {
        this.graphStore = graphStore;
        this.distanceMatrix = distanceMatrix;
        this.dijkstraModule = dijkstraModule;
        this.objectMapper = objectMapper;
    }

    /**
     * Loads the demo graph and initializes the full distance matrix on application startup.
     * Runs Dijkstra from all registered nodes to pre-populate every matrix row.
     */
    @PostConstruct
    public void load() throws IOException {
        log.info("Loading demo graph from classpath:{}", DEMO_GRAPH_PATH);
        ClassPathResource resource = new ClassPathResource(DEMO_GRAPH_PATH);

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Demo graph not found at classpath:" + DEMO_GRAPH_PATH
                    + ". Ensure the file exists in src/main/resources/graphs/");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            loadNodes(root.get("nodes"));
            loadEdges(root.get("edges"));
        }

        initializeDistanceMatrix();
        log.info("Graph loaded: {} nodes, {} edges. Distance matrix initialized.",
                graphStore.nodeCount(),
                countTotalEdges());
    }

    /** Returns the list of Node objects loaded from the demo graph JSON. */
    public List<Node> getLoadedNodes() {
        return List.copyOf(loadedNodes);
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void loadNodes(JsonNode nodesArray) {
        if (nodesArray == null || !nodesArray.isArray()) {
            throw new IllegalStateException("Invalid demo_graph.json: 'nodes' array is missing");
        }
        for (JsonNode nodeJson : nodesArray) {
            int id = nodeJson.get("id").asInt();
            String label = nodeJson.get("label").asText();
            int x = nodeJson.get("x").asInt();
            int y = nodeJson.get("y").asInt();

            NodeId nodeId = new NodeId(id);
            graphStore.addNode(nodeId);
            loadedNodes.add(new Node(nodeId, label, x, y));
        }
        log.debug("Loaded {} nodes", loadedNodes.size());
    }

    private void loadEdges(JsonNode edgesArray) {
        if (edgesArray == null || !edgesArray.isArray()) {
            throw new IllegalStateException("Invalid demo_graph.json: 'edges' array is missing");
        }
        int edgeCount = 0;
        for (JsonNode edgeJson : edgesArray) {
            NodeId from = new NodeId(edgeJson.get("from").asInt());
            NodeId to = new NodeId(edgeJson.get("to").asInt());
            int weight = edgeJson.get("weight").asInt();
            graphStore.addEdge(new Edge(from, to, weight));
            edgeCount++;
        }
        log.debug("Loaded {} edges", edgeCount);
    }

    /**
     * Runs Dijkstra from every registered node to pre-populate the full distance matrix.
     * Complexity: O(N × (V + E) log V) where N is node count — acceptable at 30-node scale.
     */
    private void initializeDistanceMatrix() {
        Map<NodeId, List<Edge>> adjacencySnapshot = buildAdjacencySnapshot();
        java.util.Set<NodeId> allNodes = graphStore.getAllNodes();

        for (NodeId sourceNode : allNodes) {
            DijkstraInput input = new DijkstraInput(adjacencySnapshot, sourceNode, allNodes);
            DijkstraOutput output = dijkstraModule.solve(input);

            double[] rowDistances = new double[distanceMatrix.capacity()];
            java.util.Arrays.fill(rowDistances, Double.MAX_VALUE);
            rowDistances[sourceNode.value()] = 0.0;

            for (Map.Entry<NodeId, Double> entry : output.distanceMap().entrySet()) {
                int targetIndex = entry.getKey().value();
                if (targetIndex < distanceMatrix.capacity()) {
                    rowDistances[targetIndex] = entry.getValue();
                }
            }

            distanceMatrix.updateRow(sourceNode, rowDistances);
        }
    }

    /**
     * Builds an adjacency snapshot from the current GraphStore for Dijkstra input.
     * Returns a mutable deep copy so the DijkstraInput record can wrap it immutably.
     */
    private Map<NodeId, List<Edge>> buildAdjacencySnapshot() {
        Map<NodeId, List<Edge>> snapshot = new HashMap<>();
        for (NodeId nodeId : graphStore.getAllNodes()) {
            snapshot.put(nodeId, new ArrayList<>(graphStore.getNeighbors(nodeId)));
        }
        return snapshot;
    }

    private long countTotalEdges() {
        return graphStore.getAllNodes().stream()
                .mapToLong(n -> graphStore.getNeighbors(n).size())
                .sum();
    }
}
