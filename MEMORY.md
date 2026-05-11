# MEMORY.md — AI-Based Delivery Optimizer
## Latest Project Context (Updated Each Session)

---

## Project Identity

| Field | Value |
|---|---|
| Project Name | AI-Based Delivery Optimizer |
| Type | Discrete-event simulation |
| Course | Computing Algorithms — Group Project |
| Plan Document | AI-Based Delivery Optimizer — Professional End-to-End Project Plan |
| Repository | RoutePulse (github: Sharawey74/RoutePulse) |
| Total Duration | 10 days |
| Current Day | 2 (complete) |
| Current Phase | Phase 2 — Engine Foundation |
| Current Branch | `day/02-simulation-engine-event-queue` (merged to main at session end) |

---

## Repository Structure

```
RoutePulse/
├── src/main/java/com/routepulse/
│   ├── platform/          (RoutepulseApplication.java — Spring Boot entry point)
│   ├── algorithm/         (pure function modules: dijkstra, greedy, dp, binpacking)
│   ├── simulation/        (engine, events, scenario, dispatcher)
│   ├── state/             (all mutable state + snapshot)
│   ├── api/               (controllers, DTOs, SSE)
│   ├── domain/            (value objects, entities)
│   ├── config/            (SimulationConfig @ConfigurationProperties)
│   └── reporting/         (ReportGenerator, ShiftReport)
├── src/main/resources/
│   ├── application.yaml   (simulation.* properties)
│   ├── graphs/            (demo_graph.json)
│   └── scenarios/         (rush_hour.json, road_closure.json)
├── src/test/java/com/routepulse/
├── frontend/              (React 18 + Vite + Tailwind + TS — created Day 7)
│   ├── src/
│   │   ├── components/
│   │   ├── context/
│   │   ├── hooks/
│   │   └── types/
│   ├── package.json
│   └── vite.config.ts
├── pom.xml                (Spring Boot 3.5.14, Java 21, Lombok)
├── docker-compose.yml     (populated Day 10)
├── Makefile               (populated Day 10)
├── SYSTEM_INSTRUCTIONS.md
├── MEMORY.md
└── PROGRESS.md
```

---

## Committed Scale Parameters

```
Graph nodes:           25–30
Graph edges:           60–80 directed, weighted (weights: 1–20)
Active couriers:       4–6
Orders per shift:      30–50
Simulation ticks:      100
DP stop cap:           10 remaining stops per courier
Brute-force cap:       5 remaining stops per courier
Cargo slots/courier:   4–5
Cargo size dist:       30% large (0.6–0.7 slot), 70% small (0.15–0.2 slot)
Event queue depth:     ≤ 200 events per run
Distance matrix:       30×30 max
```

---

## Graph Design Decision

The demo graph uses a **two-cluster topology**:
- District A: ~12 delivery nodes (left side of graph)
- District B: ~12 delivery nodes (right side of graph)
- 1–2 high-cost bridge edges connecting the clusters (weight: 15–20)
- Central depot node connecting to both clusters

**Purpose:** This topology guarantees DP outperforms greedy insertion
by at least 10% on the demonstration scenario (cluster-crossing orders),
making the quality gap chart visually distinct.

---

## Algorithm Interface Contract

Package: `com.routepulse.algorithm.api`

```java
public interface AlgorithmModule<I extends AlgorithmInput,
                                  O extends AlgorithmOutput> {
    O solve(I input);
    String algorithmName();
    AlgorithmComplexity reportedComplexity();
}
```

All four modules must implement this. The `SimulationEngine` calls
algorithms only through this interface.

---

## Event Types and Algorithm Chains

| Event | Algorithm Chain |
|---|---|
| `NEW_ORDER` | GreedyInsertion → BinPackingValidation → ShadowUpdate |
| `ROAD_WEIGHT_CHANGE` | Dijkstra(affected sources) → DistanceMatrix update |
| `ROAD_REMOVAL` | Dijkstra(affected couriers) → Route re-check |
| `VEHICLE_BREAKDOWN` | CourierDeactivate → BinPackingFFD → GreedyInsertion (per redistributed order) |
| `PRIORITY_ESCALATION` | GreedyInsertion(priority-weighted) |
| `QUIET_PERIOD` | DPReoptimiser (per eligible courier) → ShadowComparison |

**Event priority within same tick:**
1. ROAD_REMOVAL
2. ROAD_WEIGHT_CHANGE
3. VEHICLE_BREAKDOWN
4. NEW_ORDER
5. PRIORITY_ESCALATION
6. QUIET_PERIOD

---

## State Layer Objects

| Object | Responsibility | Mutated By |
|---|---|---|
| `GraphStore` | Adjacency list + edge weights | MutationApplier only |
| `DistanceMatrix` | 30×30 shortest-path cache | MutationApplier (post-Dijkstra) |
| `RouteRegistry` | Per-courier ordered stop sequences | MutationApplier only |
| `ShadowRouteRegistry` | Greedy-only parallel routes | MutationApplier only |
| `CargoRegistry` | Per-courier cargo manifests | MutationApplier only |
| `OrderRegistry` | Order lifecycle states | MutationApplier only |
| `CourierRegistry` | Courier positions and status | MutationApplier only |
| `EventLogStore` | Append-only event history | MutationApplier only |
| `MetricsStore` | Per-tick KPI snapshots | MutationApplier only |

---

## Key Design Decisions (Record Here as Made)

| Decision | Rationale | Date |
|---|---|---|
| Discrete-event simulation, not real-time | Eliminates threading; focuses on algorithms | Pre-start |
| Full Dijkstra re-run (no incremental) | Research-level complexity avoided; correct at 30-node scale | Pre-start |
| Hard DP cap at 10 stops | 2^10 = 1024 states; safe memory/time at committed scale | Pre-start |
| Sorted ArrayList for event queue (day 1–2) | Simplest correct implementation; swap to PriorityQueue day 3 | Pre-start |
| Bimodal cargo size distribution | Ensures FF vs BF vs FFD produce measurable differences | Pre-start |
| Two-cluster graph topology | Guarantees visible DP vs greedy quality gap | Pre-start |
| SSE not WebSocket | One-directional push; simpler protocol; no broker needed | Pre-start |
| No database | Ephemeral simulation state; JSON export sufficient | Pre-start |

---

## Open Issues / Blockers

_None — Day 1 and Day 2 passed all verification gates cleanly._

---

## Completed Modules

## Completed Modules

| Module | Package | Status |
|---|---|---|
| `AlgorithmModule<I,O>` interface | `com.routepulse.algorithm.api` | ✅ Complete |
| `AlgorithmInput`, `AlgorithmOutput`, `Mutation` (sealed), `MetricsRecord`, `AlgorithmComplexity` | `com.routepulse.algorithm.api` | ✅ Complete |
| `NodeId`, `CourierId`, `Distance`, `SimulatedTick` | `com.routepulse.domain` | ✅ Complete |
| `Node`, `Edge`, `Order`, `Courier`, `Route`, `Stop`, `CargoItem`, `CargoCapacity` | `com.routepulse.domain` | ✅ Complete |
| `OrderPriority`, `StopStatus`, `CourierStatus` enums | `com.routepulse.domain` | ✅ Complete |
| `SimulationConfig` | `com.routepulse.config` | ✅ Complete |
| `GraphStore` | `com.routepulse.state` | ✅ Complete |
| `DistanceMatrix` | `com.routepulse.state` | ✅ Complete |
| `GraphLoader` | `com.routepulse.state` | ✅ Complete |
| `DijkstraInput`, `DijkstraOutput`, `DijkstraMetrics` | `com.routepulse.algorithm.dijkstra` | ✅ Complete |
| `DijkstraModule` | `com.routepulse.algorithm.dijkstra` | ✅ Complete |
| `demo_graph.json` | `src/main/resources/graphs/` | ✅ Complete |
| `EventType`, `EventPriority`, `Event`, `EventPayload` | `com.routepulse.simulation.events` | ✅ Complete |
| `EventQueue`, `SimClock`, `QuietPeriodMonitor` | `com.routepulse.simulation.engine` | ✅ Complete |
| `MutationApplier`, `EventDispatcher`, `SimulationEngine` | `com.routepulse.simulation.engine` | ✅ Complete |
| `ScenarioDefinition`, `ScenarioLoader` | `com.routepulse.simulation.scenario` | ✅ Complete |

---

## Test Coverage Status

| Module | Unit Tests | Integration Tests | Status |
|---|---|---|---|
| DijkstraModule | 3/3 ✅ | — | Complete |
| GreedyInsertionModule | 0/3 | — | Not started |
| DPReoptimiserModule | 0/4 | — | Not started |
| BinPackingModule | 0/3 | — | Not started |
| SimulationEngine | 1/1 ✅ | 0/2 | Complete |
| EventDispatcher | 1/1 ✅ | 0/1 | Complete |
| SpringBootContext | — | 1/1 ✅ | Complete |

---

## Day 1 Design Decisions

| Decision | Rationale | Date |
|---|---|---|
| `Mutation` sealed interface starts with only `NoOpMutation` | Future mutation types (EdgeWeightMutation, etc.) added as new permits per OCP | Day 1 |
| `Edge.withWeight()` returns new Edge (immutable) | Records are immutable; mutation applied by MutationApplier via GraphStore | Day 1 |
| `GraphStore` returns `Collections.unmodifiableList` from `getNeighbors()` | Prevents algorithm modules from accidentally mutating live graph state | Day 1 |
| Demo graph: 28 nodes, 86 directed edges, bridge weight 18 | 2 bridge edges (13↔14, weight 18) guarantee DP outperforms greedy by >10% on cross-cluster orders | Day 1 |
| `GraphLoader` runs Dijkstra from all nodes at startup (O(N×(V+E)logV)) | Acceptable at 30-node scale; eliminates runtime latency for first-tick distance lookups | Day 1 |
| `DijkstraInput` deep-copies adjacency map and node set | Guarantees algorithm purity even if GraphStore is mutated concurrently in future | Day 1 |

## Day 2 Design Decisions

| Decision | Rationale | Date |
|---|---|---|
| `EventQueue` uses `ArrayList` with binary search insertion | Acceptable O(n) insertion cost for <200 events; simpler than PriorityQueue with dynamic rebuilding | Day 2 |
| `SimulationEngine` uses pattern-matched switch in `EventDispatcher` | Compiler guarantees all `EventType` values have a handled branch | Day 2 |
| `SimulationState` marker interface defined | Prepares for Day 3 state aggregation without coupling engine to specific registries early | Day 2 |
| `MutationApplier` handles all Day 3-6 mutations | Centralized state mutation logic enforcing the `StateLayer` vs `AlgorithmLayer` architectural rule | Day 2 |

## Last Updated

Day 2 — Simulation engine loop, event queue, dispatcher, and mutation applier skeleton. Test suite passes.
