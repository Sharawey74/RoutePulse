# SESSION — Day 1
## Domain Model + Graph + Dijkstra

---

## Pre-Session Checklist

- [ ] Read `SYSTEM_PROMPT.md` fully
- [ ] Read `MEMORY.md` fully
- [ ] Read `PROGRESS.md` — Day 1 section
- [ ] Confirm branch: `feature/domain-and-graph-day-1`
  ```
  git checkout main
  git checkout -b feature/domain-and-graph-day-1
  ```

---

## Context

**Plan reference:** Section 3 (Algorithm Design — Dijkstra),
Section 8 (System Design — Value Objects, Strategy Pattern),
Section 9 (Backend Design — Package Structure)

**Goal:** Build the domain model, the graph data structure,
the `AlgorithmModule` interface contract, and the Dijkstra
module as a verified pure function. Everything built today
is the foundation that every other module depends on.

---

## Tasks

### 1. Algorithm Interface Contract
Define in `com.deliveryoptimizer.algorithm.api`:

```
AlgorithmModule<I extends AlgorithmInput, O extends AlgorithmOutput>
AlgorithmInput     (interface)
AlgorithmOutput    (interface)
Mutation           (sealed interface)
MetricsRecord      (interface)
AlgorithmComplexity (record: name, bigONotation)
```

### 2. Value Objects
Define in `com.deliveryoptimizer.domain` using Java 21 `record`:

```
NodeId(int value)
CourierId(String value)
Distance(double kilometers)
SimulatedTick(int value) — implements Comparable
```

### 3. Domain Entities
Define in `com.deliveryoptimizer.domain`:

```
Node      — id: NodeId, label: String, x: int, y: int (canvas coords)
Edge      — from: NodeId, to: NodeId, weight: int (1–20)
Order     — id: String, deliveryNode: NodeId, weight: double,
            volume: double, priority: OrderPriority
Courier   — id: CourierId, currentNode: NodeId, capacity: CargoCapacity
Route     — courierId, stops: List<Stop>
Stop      — orderId: String, nodeId: NodeId, status: StopStatus
CargoItem — orderId: String, weight: double, volume: double
CargoCapacity — maxWeight: double, maxVolume: double
```

### 4. Graph Store
Implement `com.deliveryoptimizer.state.GraphStore`:
- Adjacency list: `Map<NodeId, List<Edge>>`
- `getNeighbors(NodeId)` — returns outgoing edges
- `updateEdgeWeight(NodeId from, NodeId to, int newWeight)` — mutates in place
- `removeEdge(NodeId from, NodeId to)` — removes from adjacency list
- Loaded from a hardcoded demo graph (25–30 nodes, two-cluster topology)

### 5. Distance Matrix
Implement `com.deliveryoptimizer.state.DistanceMatrix`:
- Internal: `double[][] matrix` (30×30)
- `get(NodeId from, NodeId to)` — O(1) lookup
- `updateRow(NodeId source, double[] newDistances)` — updates one row
- Pre-populated at initialization by running Dijkstra from all nodes

### 6. Dijkstra Module
Implement `com.deliveryoptimizer.algorithm.dijkstra.DijkstraModule`
implementing `AlgorithmModule<DijkstraInput, DijkstraOutput>`:

Algorithm:
- Standard single-source Dijkstra with a binary heap (`PriorityQueue`)
- Returns: `distMap[v]` and `predecessor[v]` for all reachable v
- `DijkstraInput`: graph snapshot, source node
- `DijkstraOutput`: distance map, predecessor map, `MetricsRecord`
- **Pure function — no side effects**

### 7. Demo Graph Definition
Define the demo graph as a JSON resource file
`src/main/resources/graphs/demo_graph.json`:
- 28 nodes minimum
- Two geographic clusters (District A: nodes 1–12, District B: nodes 14–25)
- Depot: node 0 (connected to both clusters)
- Bridge edges between clusters: 2 edges with weight 15–20
- Intra-cluster edges: weight 1–8

---

## Unit Tests (All Must Pass Before Day Ends)

File: `DijkstraModuleTest.java`

**Test 1 — Correctness on known graph:**
Build an 8-node graph with hand-calculated shortest paths.
Assert `distMap[source][dest]` matches expected value for all pairs.

**Test 2 — Disconnected nodes:**
Build a graph where node 7 is unreachable from node 0.
Assert `distMap[0][7] == Double.MAX_VALUE`.

**Test 3 — Edge weight change:**
Run Dijkstra. Change one edge weight. Re-run from affected source.
Assert paths through that edge update; all other paths unchanged.

---

## Definition of Done

- [ ] `mvn test` passes (3 tests, 0 failures)
- [ ] `DijkstraModule` has no imports from `simulation` or `api` packages
- [ ] All value objects are Java `record` types
- [ ] Class-level JavaDoc on every class stating its single responsibility
- [ ] Demo graph JSON loaded and parsed correctly on application start
- [ ] `MEMORY.md` updated: note any design decisions made today
- [ ] `PROGRESS.md` Day 1 checklist completed

---

## What NOT to Do Today

- Do not implement the simulation engine or event queue
- Do not implement `SimulationController` or any REST endpoint
- Do not implement greedy insertion, DP, or bin packing
- Do not implement incremental Dijkstra — only standard single-source

---

## Post-Session

```
git add .
git commit -m "day 1: domain model, graph store, dijkstra module, 3 tests passing"
git checkout main
git merge feature/domain-and-graph-day-1
```

Update `MEMORY.md` and `PROGRESS.md` before closing.

