# PROGRESS.md — AI-Based Delivery Optimizer
## Implementation Tracking

---

## Overall Status

| Phase | Days | Status | Branch |
|---|---|---|---|
| Phase 1 — Foundation | 1–3 | Not started | `main` |
| Phase 2 — Core Algorithms | 4–6 | Not started | `main` |
| Phase 3 — Integration | 7–8 | Not started | `main` |
| Phase 4 — Frontend | 7–9 | Not started | `main` |
| Phase 5 — Polish \u0026 Delivery | 10 | Not started | `main` |

---

## Day-by-Day Tracker

---

### Day 1 — Domain Model + Graph + Dijkstra
**Branch:** `feature/domain-and-graph-day-1` (from `main`)
**Status:** ⬜ Not started

#### Checklist
- [ ] Value objects defined: `NodeId`, `CourierId`, `Distance`, `SimulatedTick`
- [ ] Domain classes defined: `Node`, `Edge`, `Order`, `Courier`, `Route`, `Stop`, `CargoItem`
- [ ] `AlgorithmModule<I,O>` interface defined
- [ ] `AlgorithmInput`, `AlgorithmOutput`, `Mutation`, `MetricsRecord` interfaces defined
- [ ] `DijkstraInput`, `DijkstraOutput` defined
- [ ] `DijkstraModule` implemented (pure function)
- [ ] `GraphStore` implemented (adjacency list)
- [ ] 30×30 distance matrix pre-computed on init
- [ ] Unit test 1: Known 8-node graph — all shortest paths correct
- [ ] Unit test 2: Disconnected graph — unreachable nodes return MAX_VALUE
- [ ] Unit test 3: Edge weight change — only affected paths update
- [ ] All 3 Dijkstra tests passing (`mvn test`)
- [ ] No imports from simulation or API package in algorithm package
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Notes
_Fill during session_

---

### Day 2 — Simulation Engine + Event Queue
**Branch:** `feature/simulation-engine-day-2-3` (from `main`)
**Status:** ⬜ Not started

#### Checklist
- [ ] `Event` class with builder pattern
- [ ] `EventType` enum (all 6 types)
- [ ] `EventPriority` enum
- [ ] All event payload classes defined
- [ ] `SimClock` implemented
- [ ] Event queue as sorted `ArrayList` with comparator (priority + tick order)
- [ ] `SimulationEngine` with `step()`, `run(n)`, `reset()` methods
- [ ] `EventDispatcher` with no-op handlers (logs event, no algorithm call yet)
- [ ] `MutationApplier` with no-op apply (structure only)
- [ ] `QuietPeriodMonitor` implemented
- [ ] 10-event test queue — ordering verified correct
- [ ] Tick-by-tick progression logs correct event sequence
- [ ] `ScenarioLoader` reads scenario JSON
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Notes
_Fill during session_

---

### Day 3 — Disruption Events + State Layer + API Skeleton
**Branch:** `feature/simulation-engine-day-2-3` (from `main`)
**Status:** ⬜ Not started

#### Checklist
- [ ] All state layer objects implemented: `GraphStore`, `DistanceMatrix`,
  `RouteRegistry`, `CargoRegistry`, `OrderRegistry`, `CourierRegistry`,
  `ShadowRouteRegistry`, `EventLogStore`, `MetricsStore`
- [ ] `SystemStateSnapshot` (immutable) implemented
- [ ] `SnapshotFactory` implemented
- [ ] `ROAD_WEIGHT_CHANGE` handler wired: updates graph + runs Dijkstra + updates matrix
- [ ] `ROAD_REMOVAL` handler wired: removes edge + runs Dijkstra for affected couriers
- [ ] `MutationApplier` applies `EdgeWeightMutation` and `EdgeRemovalMutation`
- [ ] Spring Boot app starts with `POST /api/simulation/step`
- [ ] SSE endpoint skeleton: `GET /api/simulation/stream`
- [ ] `curl` test: step through 3 road events, distance matrix updates verified
- [ ] Dijkstra always runs before other algorithms in same tick (order enforced)
- [ ] Phase 1 merged to `develop` and `main`
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Notes
_Fill during session_

---

### Day 4 — Greedy Insertion + Order Registry + Shadow Routes
**Branch:** `feature/algorithm-modules-day-4-6` (from `main`)
**Status:** ⬜ Not started

#### Checklist
- [ ] `InsertionInput`, `InsertionOutput` defined
- [ ] `GreedyInsertionModule` implemented (O(k×m) — correct complexity)
- [ ] Insertion cost formula: `dist[prev→new] + dist[new→next] - dist[prev→next]`
- [ ] Priority-weighting variant for `PRIORITY_ESCALATION` event
- [ ] Capacity check before committing insertion
- [ ] `OrderRegistry` with pending/assigned/in-transit/delivered lifecycle
- [ ] `ShadowRouteRegistry` initialized at first insertion
- [ ] Shadow receives insertions; shadow never receives DP improvements
- [ ] `NEW_ORDER` handler fully wired: greedy → capacity check → shadow update
- [ ] `EventLogStore` records all algorithm call metrics
- [ ] Unit test 1: 3-courier, 3-stop config — correct insertion position
- [ ] Unit test 2: All couriers over capacity — returns `NoValidInsertionMutation`
- [ ] Unit test 3: Insertion cost formula — matches hand-calculated value
- [ ] All tests passing
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Notes
_Fill during session_

---

### Day 5 — Bin Packing (All 3 Strategies)
**Branch:** `feature/algorithm-modules-day-4-6` (from `main`)
**Status:** ⬜ Not started

#### Checklist
- [ ] `PackingStrategy` interface defined
- [ ] `FirstFitStrategy` implemented
- [ ] `BestFitStrategy` implemented
- [ ] `FirstFitDecreasingStrategy` implemented (sort descending first)
- [ ] `BinPackingModule` runs all 3 strategies and returns all 3 results
- [ ] Approximation ratio computed: `strategy_result / ceil(total/capacity)`
- [ ] Wasted capacity percentage computed per strategy
- [ ] `BinPackingInput`, `BinPackingOutput` defined
- [ ] `VEHICLE_BREAKDOWN` handler wired: deactivate courier → FFD redistribute →
  greedy insert each redistributed order
- [ ] `BinPackingOutput` included in `NEW_ORDER` event metrics (capacity validation only)
- [ ] Unit test 1: Known bimodal cargo on 4 couriers — FFD correct assignment
- [ ] Unit test 2: Over-capacity scenario — no strategy exceeds vehicle capacity
- [ ] Unit test 3: All 3 strategies on same input — FFD ≤ FF on approximation ratio
- [ ] All tests passing
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Notes
_Fill during session_

---

### Day 6 — DP Re-optimizer + Brute-Force Verifier
**Branch:** `feature/algorithm-modules-day-4-6` (from `main`)
**Status:** ⬜ Not started

#### Checklist
- [ ] `DPInput`, `DPOutput` defined
- [ ] `DPOutput.deferred(reason)` factory method implemented
- [ ] `DPReoptimiserModule` implemented: held-karp `dp[S][v]` formulation
- [ ] Base case: `dp[{s}][s] = dist[current_position][s]` for each stop s
- [ ] Transition: `dp[S∪{u}][u] = min(dp[S][v] + dist[v][u])` for all v in S
- [ ] Backtracking: optimal ordering recovered via predecessor map
- [ ] Time budget check at end of each row (not inside inner loops)
- [ ] Hard cap: returns `deferred()` if stops > 10
- [ ] `BruteForceVerifier` implemented: enumerates all (n-1)! orderings for n ≤ 5
- [ ] Verification test 1: n=4 stops — DP matches brute-force
- [ ] Verification test 2: n=5 stops — DP matches brute-force
- [ ] Verification test 3: n=3 distinct configs — all match
- [ ] Verification test 4: n=10 stops — DP completes within 200ms
- [ ] Verification test 5: n=11 stops — returns `deferred()`
- [ ] `QUIET_PERIOD` handler wired: selects eligible couriers by improvement gap
- [ ] `QuietPeriodMonitor` fires correctly after N quiet ticks
- [ ] All tests passing
- [ ] Phase 2 merged to `develop` and `main`
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Notes
_Fill during session_

---

### Day 7 — Backend: Full Integration + SSE | Frontend: Setup + SSE
**Two parallel branches:**
- Backend: `feature/backend-integration-metrics-day-7-8` (from `main`)
- Frontend: `feature/frontend-ui-day-7-10` (from `main`)

**Status:** ⬜ Not started

#### Backend Checklist
- [ ] All 6 event handlers fully wired in `EventDispatcher`
- [ ] Event priority ordering enforced (road events before order events)
- [ ] `StateEmitter` emitting full `StateSnapshotDto` via SSE after each tick
- [ ] All REST endpoints implemented and verified:
  - `POST /api/simulation/init`
  - `POST /api/simulation/step`
  - `POST /api/simulation/run`
  - `POST /api/simulation/pause`
  - `POST /api/simulation/reset`
  - `POST /api/events/inject`
  - `GET /api/simulation/state`
  - `GET /api/simulation/stream` (SSE)
- [ ] Full 100-tick scripted scenario runs end-to-end without error
- [ ] All state transitions produce correct output (verified via logs)
- [ ] Integration test 1: Road removal + new order in same tick — Dijkstra before greedy
- [ ] Integration test 2: Shadow route distance ≥ active route distance after each DP cycle
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Frontend Checklist
- [ ] Vite + React 18 + TypeScript + Tailwind initialized
- [ ] `SimulationProvider` with `EventSource` SSE connection
- [ ] `useSimulation()` custom hook consuming context
- [ ] All DTO TypeScript types defined (matching backend DTOs)
- [ ] `ControlPanel` with Step, Run, Pause, Reset buttons
- [ ] REST calls wired to backend endpoints
- [ ] `stateUpdate` SSE event updates React state
- [ ] `simulationComplete` SSE event handled
- [ ] Browser console shows clean state updates per tick
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Notes
_Fill during session_

---

### Day 8 — Backend: Metrics + Report | Frontend: Map + Event Log
**Two parallel branches:**
- Backend: `feature/backend-integration-metrics-day-7-8` (from `main`)
- Frontend: `feature/frontend-ui-day-7-10` (from `main`)

**Status:** ⬜ Not started

#### Backend Checklist
- [ ] `MetricsStore` tick-by-tick snapshot accumulation implemented
- [ ] `distance_per_remaining_stop` metric tracked per courier
- [ ] Shadow route distance tracked alongside active route distance
- [ ] Quality gap percentage computed each tick
- [ ] `ReportGenerator` produces complete `ShiftReport`
- [ ] `GET /api/reports/shift` returns full report JSON
- [ ] `GET /api/reports/shift/export` returns downloadable JSON file
- [ ] Report includes: summary, per-algorithm performance, bin packing
  comparison, DP verification log
- [ ] Full scenario run → report has all fields with sensible values
- [ ] Phase 3 merged to `develop` and `main`
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Frontend Checklist
- [ ] `DeliveryMapCanvas` renders all nodes as labeled circles
- [ ] All edges rendered as weighted lines (color: green→yellow→red by weight)
- [ ] Per-courier route polylines rendered (fixed color palette, 6 colors)
- [ ] Courier position markers (colored dots at current node)
- [ ] Map redraws correctly after every tick via `useEffect`
- [ ] `EventLogPanel` with virtualized scrolling (no DOM lag at 200 events)
- [ ] Each log entry: tick, event type, courier, algorithm, time ms, cost delta
- [ ] Event type color coding applied
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Notes
_Fill during session_

---

### Day 9 — Frontend: Charts + Metrics Panels + Report Modal
**Branch:** `feature/frontend-ui-day-7-10` (from `main`)
**Status:** ⬜ Not started

#### Checklist
- [ ] `RouteQualityChart` (Chart.js line): per-courier active vs. shadow distance
  per remaining stop — streams data per tick
- [ ] `GreedyVsDPPanel`: summary cards (total saved, cycles completed,
  avg improvement %, avg DP time ms) + small bar chart
- [ ] `ExecutionTimeChart`: per-algorithm execution time over simulation
- [ ] `BinPackingPanel`: slot-fill bar per courier (stacked bars by order,
  gray = wasted capacity)
- [ ] `StrategyComparisonTable`: FF vs BF vs FFD — wasted %, approx ratio,
  couriers used
- [ ] `DPTableViewer`: renders `dp[S][v]` table on demand for selected courier
- [ ] `ReportModal`: full-screen overlay triggered by `simulationComplete` SSE
  — summary grid, quality chart, export button
- [ ] Export button downloads report JSON file
- [ ] Phase 4 merged to `develop` and `main`
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` updated

#### Notes
_Fill during session_

---

### Day 10 — Scenarios + Polish + Delivery
**Branch:** `feature/frontend-ui-day-7-10` (from `main`)
**Status:** ⬜ Not started

#### Checklist
- [ ] Scenario 1: "Rush Hour Congestion" — multiple road weight increases
  in sequence, demonstrates repeated Dijkstra re-runs and route degradation
- [ ] Scenario 2: "Road Closure Mid-Delivery" — active courier route
  disrupted, demonstrates re-routing + greedy re-insertion
- [ ] Both scenarios trigger visibly distinct algorithm behaviors on chart
- [ ] Quality gap ≥ 10% by tick 60 on at least one scenario (verified)
- [ ] Scenario selector in `ControlPanel` works for both scenarios
- [ ] Algorithm strategy toggle (Greedy Only / DP Only / Adaptive) implemented
  in dispatcher — one-flag change, no algorithm logic change
- [ ] Manual event injection form functional (road closure, new order,
  vehicle breakdown)
- [ ] All panels populated on demo run with sensible values
- [ ] `docker-compose up` builds and runs cleanly from project root
- [ ] `Makefile` with `build`, `up`, `down`, `test` targets verified
- [ ] README written: setup, usage, algorithm descriptions, complexity analysis
- [ ] Algorithm comparison report written (required course deliverable)
- [ ] One complete simulation run exported as demonstration artifact
- [ ] All unit tests passing: `mvn test`
- [ ] Phase 5 merged to `develop` and `main`
- [ ] `MEMORY.md` updated with final project state
- [ ] `PROGRESS.md` marked complete for all days

#### Notes
_Fill during session_

---

## Determinism Test (Run on Day 10)

Run Scenario 1 twice with the same seed. Assert:
- [ ] Event log is byte-identical across both runs
- [ ] All metric values are identical across both runs
- [ ] Final state snapshot is identical across both runs

---

## Summary Table

| Day | Focus | Tests | Status |
|---|---|---|---|
| 1 | Domain + Graph + Dijkstra | 3 unit | ⬜ |
| 2 | Simulation Engine | Manual verification | ⬜ |
| 3 | State Layer + API skeleton | curl verification | ⬜ |
| 4 | Greedy Insertion + Shadow Routes | 3 unit | ⬜ |
| 5 | Bin Packing (3 strategies) | 3 unit | ⬜ |
| 6 | DP + Brute-Force Verifier | 5 unit | ⬜ |
| 7 | Full Integration + Frontend Setup | 2 integration | ⬜ |
| 8 | Metrics + Report + Map + Event Log | End-to-end | ⬜ |
| 9 | Frontend Charts + Panels + Report | Visual verification | ⬜ |
| 10 | Scenarios + Polish + Delivery | Full demo run | ⬜ |
