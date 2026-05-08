# SESSION — Day 4
## Greedy Insertion + Order Registry + Shadow Routes

---

## Pre-Session Checklist

- [ ] Read `MEMORY.md` — confirm Phase 1 decisions
- [ ] Read `PROGRESS.md` — Phase 1 complete, Day 4 section
- [ ] Confirm all previous tests pass: `mvn test`
- [ ] Confirm branch: `feature/algorithm-modules-day-4-6`
  ```
  git checkout main
  git checkout feature/algorithm-modules-day-4-6
  ```

---

## Context

**Plan reference:** Section 3 (Algorithm Design — Greedy Insertion),
Section 4 (Event Types — NEW_ORDER, PRIORITY_ESCALATION),
Section 5 (Functional Requirements — FR-ALG-02, FR-STATE-01/02),
Section 8 (System Design — Shadow Route pattern)

**Goal:** Implement and verify the greedy cheapest insertion module,
wire it to the `NEW_ORDER` event, initialize the shadow route system,
and prove the O(k×m) complexity claim with a measured test.

---

## Tasks

### 1. Greedy Insertion Module
Implement `com.deliveryoptimizer.algorithm.greedy.GreedyInsertionModule`
implementing `AlgorithmModule<InsertionInput, InsertionOutput>`.

**Algorithm (insertion cost formula):**
```
For each courier C with route [c0, c1, ..., cn]:
  For each position i from 1 to n+1:
    cost(i) = dist[c_{i-1}][d] + dist[d][c_i] - dist[c_{i-1}][c_i]
    where d = new order delivery node
    and dist values come from DistanceMatrix

Select (courier, position) with global minimum cost
where courier has sufficient remaining capacity
```

**Priority-weighted variant:**
```
weighted_cost(i) = cost(i) + PRIORITY_WEIGHT * estimated_delay_ticks(i)
```
Used only for `PRIORITY_ESCALATION` events. `PRIORITY_WEIGHT` is a
constant in `SimulationConfig`.

**Inputs:**
- Current routes snapshot
- New order
- Distance matrix
- (Optional) priority weight flag

**Output:**
- `InsertionOutput`: target courier, insertion position, updated route,
  detour cost, positions evaluated count, execution time ms
- OR: `InsertionOutput.noValidInsertion()` if all couriers over capacity

**Complexity:** O(k × m) where k = active couriers, m = avg stops.
State this in the class JavaDoc and in a comment at the loop head.

### 2. Order Registry — Full Implementation
Complete `com.deliveryoptimizer.state.OrderRegistry`:
- Add `assignOrder(orderId, courierId)` — transitions PENDING â†’ ASSIGNED
- Add `startDelivery(orderId)` — transitions ASSIGNED â†’ IN_TRANSIT
- Add `completeDelivery(orderId)` — transitions IN_TRANSIT â†’ DELIVERED
- All transitions validated — invalid transitions throw
  `IllegalStateTransitionException` with descriptive message

### 3. Shadow Route Registry — Full Implementation
`ShadowRouteRegistry` mirrors `RouteRegistry` structure.

**Rules (enforce via access modifiers):**
- Shadow is initialized in `SimulationEngine.init()` to match the
  initial route assignments
- Shadow receives `RouteInsertionMutation` from greedy insertion
- Shadow **never** receives `RouteReorderMutation` from DP
- Both routes receive delivery completion removals (completed stops
  are removed from both)

### 4. Wire NEW_ORDER Handler
In `EventDispatcher`, implement `handleNewOrder(NewOrderPayload)`:

```
1. Take SystemStateSnapshot
2. Call GreedyInsertionModule.solve(InsertionInput)
3. If noValidInsertion â†’ log warning, return NoOpMutation
4. Call BinPackingModule.validateCapacity(...) [DAY 5 — stub today]
5. Update OrderRegistry: PENDING â†’ ASSIGNED
6. Return composite mutation: RouteInsertionMutation + ShadowInsertionMutation
```

**Today:** Step 4 is a stub that always returns "capacity valid."
Real bin packing implementation is day 5.

### 5. Wire PRIORITY_ESCALATION Handler
```
1. Find order's current courier and position in route
2. Call GreedyInsertionModule.solve() with priority weight flag
3. If new position is earlier than current â†’ return RouteReorderMutation
4. If no improvement â†’ return NoOpMutation (log: "escalation no improvement")
```

### 6. Normalized Quality Metric
Add to `MetricsStore.recordTick()`:
```
distance_per_remaining_stop = total_remaining_distance / remaining_stop_count
```
Computed for both active route and shadow route per courier.
This is the only route quality metric used in the comparison chart.

### 7. EventLogStore Entry for Algorithm Calls
Every algorithm call must produce an `EventLogEntry`:
```java
record EventLogEntry(
    SimulatedTick tick,
    EventType eventType,
    String algorithmName,
    String affectedCourierId,
    double executionTimeMs,
    double solutionCost,
    double costDelta,         // vs. pre-event cost
    int statesEvaluated,
    String notes
) {}
```

---

## Unit Tests

File: `GreedyInsertionModuleTest.java`

**Test 1 — Correct insertion position:**
Build a 3-courier, 3-stop-each snapshot. Add one new order.
Hand-calculate the optimal insertion position.
Assert module selects the correct (courier, position) pair.

**Test 2 — All couriers over capacity:**
Fill all couriers to max capacity.
Assert `output.isNoValidInsertion() == true`.

**Test 3 — Insertion cost formula:**
For a known 4-node route, compute expected detour cost manually.
Assert `output.detourCost()` matches within 0.001 tolerance.

**Test 4 — Priority weighting biases toward earlier positions:**
Create a route where a standard insertion would place the order last.
Set priority weight high. Assert the priority-weighted insertion
places it earlier in the route (higher detour cost accepted).

---

## Definition of Done

- [ ] `mvn test` — all 4 greedy tests pass
- [ ] `NEW_ORDER` event processed in simulation without errors
- [ ] Shadow route initialized and updated correctly after first order
- [ ] Shadow distance â‰¥ active distance (or equal if no DP has run)
- [ ] `EventLogEntry` recorded for every greedy insertion call
- [ ] Complexity stated as O(k×m) in JavaDoc — not O(n)
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` Day 4 checklist completed

---

## What NOT to Do Today

- Do not implement real bin packing validation (stub only)
- Do not implement DP — that is day 6
- Do not implement frontend
- Do not change graph store or distance matrix structure

---

## Post-Session

```
git add .
git commit -m "day 4: greedy insertion module, order registry, shadow routes, 4 tests"
git checkout main
git merge feature/algorithm-modules-day-4-6
```

Update `MEMORY.md` and `PROGRESS.md` before closing.

