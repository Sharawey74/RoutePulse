package com.routepulse.algorithm.dijkstra;

import com.routepulse.domain.Edge;
import com.routepulse.domain.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link DijkstraModule}.
 *
 * <p>Test strategy: all tests construct their own in-memory adjacency snapshots
 * with hand-calculated expected values. No Spring context is loaded — the module
 * is instantiated directly as a plain Java object, proving its purity.
 *
 * <p>Tests:
 * <ol>
 *   <li>Correctness on a known 8-node graph — all shortest paths verified</li>
 *   <li>Disconnected graph — unreachable nodes return {@code Double.MAX_VALUE}</li>
 *   <li>Edge weight change — re-run from affected source updates only that row</li>
 * </ol>
 */
@DisplayName("DijkstraModule — Unit Tests")
class DijkstraModuleTest {

    private DijkstraModule dijkstraModule;

    @BeforeEach
    void setUp() {
        dijkstraModule = new DijkstraModule();
    }

    // ── Test 1: Correctness on known 8-node graph ───────────────────────────

    /**
     * Test 1 — Correctness on a known 8-node directed weighted graph.
     *
     * <p>Graph topology (directed edges with weights):
     * <pre>
     *   0 → 1 (4)    1 → 2 (3)    2 → 5 (2)
     *   0 → 2 (8)    1 → 3 (6)    3 → 4 (2)
     *   0 → 4 (3)    4 → 5 (1)    4 → 6 (7)
     *   5 → 6 (3)    6 → 7 (4)    3 → 7 (9)
     * </pre>
     *
     * <p>Hand-calculated shortest paths from node 0:
     * <ul>
     *   <li>0→0: 0</li>
     *   <li>0→1: 4</li>
     *   <li>0→2: 7  (0→1→2)</li>
     *   <li>0→3: 10 (0→1→3)</li>
     *   <li>0→4: 3  (0→4 direct)</li>
     *   <li>0→5: 4  (0→4→5)</li>
     *   <li>0→6: 7  (0→4→5→6)</li>
     *   <li>0→7: 11 (0→4→5→6→7)</li>
     * </ul>
     */
    @Test
    @DisplayName("Test 1: Correct shortest paths on 8-node hand-calculated graph")
    void testCorrectShortestPaths_knownGraph() {
        // Arrange — build 8-node graph
        Map<NodeId, List<Edge>> adjacency = new HashMap<>();
        Set<NodeId> nodes = buildNodeSet(8);
        for (NodeId n : nodes) {
            adjacency.put(n, new ArrayList<>());
        }

        addEdge(adjacency, 0, 1, 4);
        addEdge(adjacency, 0, 2, 8);
        addEdge(adjacency, 0, 4, 3);
        addEdge(adjacency, 1, 2, 3);
        addEdge(adjacency, 1, 3, 6);
        addEdge(adjacency, 2, 5, 2);
        addEdge(adjacency, 3, 4, 2);
        addEdge(adjacency, 3, 7, 9);
        addEdge(adjacency, 4, 5, 1);
        addEdge(adjacency, 4, 6, 7);
        addEdge(adjacency, 5, 6, 3);
        addEdge(adjacency, 6, 7, 4);

        DijkstraInput input = new DijkstraInput(adjacency, new NodeId(0), nodes);

        // Act
        DijkstraOutput output = dijkstraModule.solve(input);

        // Assert — all hand-calculated values
        assertThat(output.distanceTo(new NodeId(0))).isEqualTo(0.0);
        assertThat(output.distanceTo(new NodeId(1))).isCloseTo(4.0, within(0.001));
        assertThat(output.distanceTo(new NodeId(2))).isCloseTo(7.0, within(0.001));
        assertThat(output.distanceTo(new NodeId(3))).isCloseTo(10.0, within(0.001));
        assertThat(output.distanceTo(new NodeId(4))).isCloseTo(3.0, within(0.001));
        assertThat(output.distanceTo(new NodeId(5))).isCloseTo(4.0, within(0.001));
        assertThat(output.distanceTo(new NodeId(6))).isCloseTo(7.0, within(0.001));
        assertThat(output.distanceTo(new NodeId(7))).isCloseTo(11.0, within(0.001));

        // Predecessor verification for key nodes
        assertThat(output.predecessorMap().get(new NodeId(5))).isEqualTo(new NodeId(4));
        assertThat(output.predecessorMap().get(new NodeId(6))).isEqualTo(new NodeId(5));
        assertThat(output.predecessorMap().get(new NodeId(7))).isEqualTo(new NodeId(6));

        // Metrics sanity
        assertThat(output.metrics().nodesVisited()).isGreaterThan(0);
        assertThat(output.metrics().edgesRelaxed()).isGreaterThan(0);
        assertThat(output.metrics().sourceNodeId()).isEqualTo(0);
    }

    // ── Test 2: Disconnected graph — unreachable nodes ──────────────────────

    /**
     * Test 2 — Disconnected graph where node 7 is completely isolated.
     *
     * <p>Graph: nodes 0–7, with edges only among 0–6. Node 7 has no incoming edges.
     * Expected: dist[0][7] == Double.MAX_VALUE (unreachable).
     * All other reachable nodes have finite distances.
     */
    @Test
    @DisplayName("Test 2: Unreachable node returns Double.MAX_VALUE in disconnected graph")
    void testDisconnectedGraph_unreachableNodeReturnsMaxValue() {
        // Arrange — connected component: nodes 0–6; isolated island: node 7
        Map<NodeId, List<Edge>> adjacency = new HashMap<>();
        Set<NodeId> nodes = buildNodeSet(8);
        for (NodeId n : nodes) {
            adjacency.put(n, new ArrayList<>());
        }

        // Connected component (0→6)
        addEdge(adjacency, 0, 1, 2);
        addEdge(adjacency, 1, 2, 3);
        addEdge(adjacency, 2, 3, 1);
        addEdge(adjacency, 3, 4, 4);
        addEdge(adjacency, 4, 5, 2);
        addEdge(adjacency, 5, 6, 3);
        // Node 7 is intentionally isolated — no edges from any node to node 7

        DijkstraInput input = new DijkstraInput(adjacency, new NodeId(0), nodes);

        // Act
        DijkstraOutput output = dijkstraModule.solve(input);

        // Assert — node 7 must be unreachable
        assertThat(output.distanceTo(new NodeId(7)))
                .as("Node 7 is isolated — distance must be Double.MAX_VALUE")
                .isEqualTo(Double.MAX_VALUE);
        assertThat(output.isReachable(new NodeId(7))).isFalse();

        // All other nodes must be reachable
        for (int i = 0; i <= 6; i++) {
            assertThat(output.isReachable(new NodeId(i)))
                    .as("Node %d should be reachable from node 0", i)
                    .isTrue();
        }
    }

    // ── Test 3: Edge weight change — re-run reflects updated path ───────────

    /**
     * Test 3 — Edge weight change causes re-run to produce updated distances.
     *
     * <p>Initial graph has edge 0→2 with weight 1 (direct fast path).
     * After changing edge 0→2 weight to 20, the shortest path from 0 to 2
     * goes through node 1 instead: 0→1→2 with cost 3+3=6, not 20.
     *
     * <p>Asserts:
     * <ul>
     *   <li>Before change: dist[0][2] = 1 (direct edge)</li>
     *   <li>After change: dist[0][2] = 6 (via node 1)</li>
     *   <li>Paths not involving the changed edge are unaffected</li>
     * </ul>
     */
    @Test
    @DisplayName("Test 3: Edge weight change causes correct re-computation via new shortest path")
    void testEdgeWeightChange_reRunUpdatesOnlyAffectedPaths() {
        // Arrange — initial graph: 0→2 is the fastest path (weight 1)
        Map<NodeId, List<Edge>> adjacencyBefore = new HashMap<>();
        Set<NodeId> nodes = buildNodeSet(5);
        for (NodeId n : nodes) {
            adjacencyBefore.put(n, new ArrayList<>());
        }

        addEdge(adjacencyBefore, 0, 1, 3);
        addEdge(adjacencyBefore, 1, 2, 3);
        addEdge(adjacencyBefore, 0, 2, 1);   // Fast direct path
        addEdge(adjacencyBefore, 2, 3, 2);
        addEdge(adjacencyBefore, 3, 4, 5);

        // Act — first run with fast direct edge 0→2 (weight 1)
        DijkstraInput inputBefore = new DijkstraInput(adjacencyBefore, new NodeId(0), nodes);
        DijkstraOutput outputBefore = dijkstraModule.solve(inputBefore);

        assertThat(outputBefore.distanceTo(new NodeId(2)))
                .as("Before weight change: direct edge 0→2 has weight 1")
                .isCloseTo(1.0, within(0.001));

        // Arrange — simulate ROAD_WEIGHT_CHANGE: edge 0→2 weight changes to 20
        Map<NodeId, List<Edge>> adjacencyAfter = new HashMap<>();
        for (NodeId n : nodes) {
            adjacencyAfter.put(n, new ArrayList<>());
        }
        addEdge(adjacencyAfter, 0, 1, 3);
        addEdge(adjacencyAfter, 1, 2, 3);
        addEdge(adjacencyAfter, 0, 2, 20);   // Changed to high cost
        addEdge(adjacencyAfter, 2, 3, 2);
        addEdge(adjacencyAfter, 3, 4, 5);

        // Act — second run after weight change
        DijkstraInput inputAfter = new DijkstraInput(adjacencyAfter, new NodeId(0), nodes);
        DijkstraOutput outputAfter = dijkstraModule.solve(inputAfter);

        // Assert — path via node 1 (cost 6) is now shorter than direct (cost 20)
        assertThat(outputAfter.distanceTo(new NodeId(2)))
                .as("After weight change: path 0→1→2 (cost 6) beats 0→2 direct (cost 20)")
                .isCloseTo(6.0, within(0.001));

        // Predecessor must now be node 1, not node 0
        assertThat(outputAfter.predecessorMap().get(new NodeId(2)))
                .as("Predecessor of node 2 should now be node 1 (via detour)")
                .isEqualTo(new NodeId(1));

        // Paths not through the changed edge must be unaffected (same in both runs)
        assertThat(outputAfter.distanceTo(new NodeId(1)))
                .as("Distance to node 1 is unchanged (still 3)")
                .isCloseTo(outputBefore.distanceTo(new NodeId(1)), within(0.001));

        // Downstream paths through node 2 are updated correctly
        assertThat(outputAfter.distanceTo(new NodeId(3)))
                .as("Distance to node 3 now goes via node 1: 6+2=8")
                .isCloseTo(8.0, within(0.001));
        assertThat(outputAfter.distanceTo(new NodeId(4)))
                .as("Distance to node 4 now goes via node 1: 6+2+5=13")
                .isCloseTo(13.0, within(0.001));
    }

    // ── Private test helpers ─────────────────────────────────────────────────

    private Set<NodeId> buildNodeSet(int count) {
        java.util.HashSet<NodeId> nodes = new java.util.HashSet<>();
        for (int i = 0; i < count; i++) {
            nodes.add(new NodeId(i));
        }
        return nodes;
    }

    private void addEdge(Map<NodeId, List<Edge>> adjacency, int from, int to, int weight) {
        adjacency.get(new NodeId(from)).add(new Edge(new NodeId(from), new NodeId(to), weight));
    }
}
