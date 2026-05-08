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
| Total Duration | 10 days |
| Current Day | 0 (not started) |
| Current Phase | Pre-implementation |
| Current Branch | — |

---

## Repository Structure

```
delivery-optimizer/
├── backend/               (Java 21 + Spring Boot 3.3)
│   ├── src/main/java/com/deliveryoptimizer/
│   │   ├── algorithm/     (pure function modules)
│   │   ├── simulation/    (engine, events, scenario)
│   │   ├── state/         (all mutable state)
│   │   ├── api/           (controllers, DTOs, SSE)
│   │   ├── domain/        (value objects, entities)
│   │   ├── config/        (Spring config, params)
│   │   └── reporting/     (report generation)
│   ├── src/test/java/
│   └── pom.xml
├── frontend/              (React 18 + Vite + Tailwind + TS)
│   ├── src/
│   │   ├── components/
│   │   ├── context/
│   │   ├── hooks/
│   │   └── types/
│   ├── package.json
│   └── vite.config.ts
├── docker-compose.yml
├── Makefile
├── SYSTEM_PROMPT.md
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

_None — project not started_

---

## Completed Modules

_None — project not started_

---

## Test Coverage Status

| Module | Unit Tests | Integration Tests | Status |
|---|---|---|---|
| DijkstraModule | 0/3 | — | Not started |
| GreedyInsertionModule | 0/3 | — | Not started |
| DPReoptimiserModule | 0/4 | — | Not started |
| BinPackingModule | 0/3 | — | Not started |
| SimulationEngine | — | 0/2 | Not started |
| EventDispatcher | — | 0/1 | Not started |

---

## Last Updated

Day 0 — Initial setup. Project not yet started.
