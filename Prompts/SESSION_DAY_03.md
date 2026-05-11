# SESSION — Day 3
## Disruption Events + State Layer + API Skeleton

---

## Pre-Session Checklist

- [ ] Read `MEMORY.md` — confirm Days 1–2 decisions
- [ ] Read `PROGRESS.md` — Day 2 complete, Day 3 section
- [ ] Confirm all previous tests pass: `mvn test`
- [ ] Confirm branch: `day/03-state-layer-api-skeleton`
  ```bash
  git checkout day/03-state-layer-api-skeleton
  ```

---

## Context

**Plan reference:** Section 7 (Architecture — State Layer),
Section 8 (System Design — Snapshot Pattern, Mutation Strategy),
Section 9 (Backend Design — REST API, SSE Skeleton),
Section 11 (Storage — In-Memory State Model)

**Goal:** Build the full state layer, wire the first two live
event handlers (road events + Dijkstra), add the Spring Boot
application skeleton with one working endpoint, and close
Phase 1. By end of today, `curl POST /api/simulation/step`
must process a road event and update the distance matrix.

---

## Tasks

### 1. State Layer Objects
Implement all in `com.routepulse.state`:

**`GraphStore`** — already partially built day 1; ensure it is the
authoritative graph mutation target. Wrap all mutations in package-
scoped methods (only `MutationApplier` calls mutation methods).

**`DistanceMatrix`** — 30×30 `double[][]`. Methods:
- `get(NodeId from, NodeId to)` -> double
- `updateRow(int sourceIndex, double[] distances)` — package-scoped
- `getRow(int sourceIndex)` -> `double[]`

**`RouteRegistry`** — `Map<CourierId, Route>`:
- `getRoute(CourierId)` -> `Route`
- `updateRoute(CourierId, Route)` — package-scoped
- `getAllCouriers()` -> `Set<CourierId>`

**`ShadowRouteRegistry`** — identical structure to `RouteRegistry`,
updated only by greedy insertions, never by DP.

**`CargoRegistry`** — `Map<CourierId, List<CargoItem>>`:
- `getManifest(CourierId)` -> `List<CargoItem>`
- `getRemainingCapacity(CourierId)` -> `CargoCapacity`

**`OrderRegistry`** — `Map<String, OrderStatus>`:
- `OrderStatus` enum: PENDING, ASSIGNED, IN_TRANSIT, DELIVERED
- `getStatus(String orderId)` -> `OrderStatus`
- `transition(String orderId, OrderStatus newStatus)` — package-scoped

**`CourierRegistry`** — `Map<CourierId, Courier>`:
- `getActiveCouriers()` -> `List<Courier>`
- `deactivate(CourierId)` — package-scoped

**`EventLogStore`** — append-only `List<EventLogEntry>`:
- `append(EventLogEntry entry)` — package-scoped
- `getAll()` -> unmodifiable list
- `getLast(int n)` -> last n entries

**`MetricsStore`** — `List<TickSnapshot>`:
- `recordTick(TickSnapshot snapshot)` — package-scoped
- `getAll()` -> unmodifiable list

### 2. SystemStateSnapshot (Immutable)
Implement `com.routepulse.state.snapshot.SystemStateSnapshot`
as an immutable deep copy of the current state. Algorithm modules
receive this — never the live mutable state.

```java
public record SystemStateSnapshot(
    Map<NodeId, List<Edge>> graphSnapshot,
    double[][] distanceMatrix,           // defensive copy
    Map<CourierId, Route> routes,
    Map<CourierId, Route> shadowRoutes,
    Map<CourierId, List<CargoItem>> cargo,
    Map<String, OrderStatus> orders,
    List<Courier> activeCouriers,
    int currentTick
) {}
```

### 3. SnapshotFactory
Implement `com.routepulse.state.snapshot.SnapshotFactory` — produces
`SystemStateSnapshot` by deep-copying all mutable state.
Used by `SimulationEngine` after every tick.

### 4. MutationApplier — Wire Real Mutations
Now implement real mutation logic for the two road mutation types:

```java
case EdgeWeightMutation m -> {
    state.graphStore().updateEdgeWeight(m.from(), m.to(), m.newWeight());
    // Dijkstra re-run happens in EventDispatcher before this
}
case EdgeRemovalMutation m -> {
    state.graphStore().removeEdge(m.from(), m.to());
}
```

### 5. Event Dispatcher — Wire Road Event Handlers

**`ROAD_WEIGHT_CHANGE` handler:**
1. Update edge weight in `GraphStore`
2. Identify affected source nodes (couriers whose current path
   uses the changed edge — check via predecessor maps)
3. Run `DijkstraModule.solve()` from each affected source
4. Return `DistanceMatrixUpdateMutation` with new row values

**`ROAD_REMOVAL` handler:**
1. Identify couriers whose active route includes the removed edge
2. Remove edge from `GraphStore`
3. Run `DijkstraModule.solve()` from each affected courier position
4. Return `DistanceMatrixUpdateMutation` + `RouteRecheckMutation`

**Important:** `DijkstraModule` must be injected into `EventDispatcher`
via constructor (dependency injection, not `new`).

### 6. Spring Boot Application
Set up `com.routepulse.platform.RoutepulseApplication`.

**`SimulationController`:**
```
POST /api/simulation/init    — load scenario, initialize state
POST /api/simulation/step    — process one tick, return StateSnapshotDto
POST /api/simulation/reset   — reinitialize to tick 0
```

**`SseController`:**
```
GET  /api/simulation/stream  — returns SseEmitter (skeleton only)
```

**DTOs** in `com.routepulse.api.dto`:
- `StateSnapshotDto` — JSON-serializable version of `SystemStateSnapshot`
- `CourierDto`, `RouteDto`, `EdgeDto`, `OrderDto`

### 7. SimulationConfig
Define `com.routepulse.config.SimulationConfig` as a
`@ConfigurationProperties` class:

```java
@ConfigurationProperties(prefix = "simulation")
public record SimulationConfig(
    int maxTicks,
    int dpStopCap,
    int quietPeriodTicks,
    int dpBudgetMs,
    int maxCouriers,
    int bruteForceCapStops,
    double priorityWeight,
    double minImprovementThreshold
) {}
```

Defaults in `application.yml`:
```yaml
simulation:
  max-ticks: 100
  dp-stop-cap: 10
  quiet-period-ticks: 5
  dp-budget-ms: 200
  max-couriers: 6
  brute-force-cap-stops: 5
```

---

## Verification

Use `curl` to verify (not JUnit):

```bash
# Initialize simulation
curl -X POST http://localhost:8080/api/simulation/init \
     -H "Content-Type: application/json" \
     -d '{"scenario": "demo"}'

# Step once (should process first pre-scheduled event)
curl -X POST http://localhost:8080/api/simulation/step

# Inject road weight change
curl -X POST http://localhost:8080/api/events/inject \
     -H "Content-Type: application/json" \
     -d '{"type": "ROAD_WEIGHT_CHANGE", "from": 3, "to": 7, "newWeight": 18}'

# Step — should trigger Dijkstra, distance matrix updated
curl -X POST http://localhost:8080/api/simulation/step
```

Verify in application logs that:
1. Dijkstra ran from the affected source
2. Distance matrix row was updated
3. State snapshot reflects the new edge weight

---

## Definition of Done

- [ ] All state layer objects implemented with package-scoped mutation methods
- [ ] `SystemStateSnapshot` is truly immutable (defensive copies)
- [ ] `ROAD_WEIGHT_CHANGE` and `ROAD_REMOVAL` handlers produce correct mutations
- [ ] Dijkstra runs **before** any other handler in the same tick
- [ ] Spring Boot app starts: `mvn spring-boot:run`
- [ ] `curl` verification steps produce correct log output
- [ ] Phase 1 branch merges: `day/01-domain-graph-dijkstra` -> `day/03-engine-state-dtos` -> `main`
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` Day 3 checklist completed

---

## What NOT to Do Today

- Do not implement greedy insertion, DP, or bin packing handlers
- Do not implement SSE push logic (skeleton only)
- Do not implement the frontend
- Do not add any new algorithm beyond Dijkstra

---

## Phase 1 Merge Protocol (End of Day 3)

```bash
git add .
git commit -m "day 3: state layer, road event handlers, spring boot skeleton"
git push origin day/03-state-layer-api-skeleton
git checkout main
git merge day/03-state-layer-api-skeleton
git tag v0.1-phase1-complete
```

Update `MEMORY.md` and `PROGRESS.md` before closing.

