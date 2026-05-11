# SESSION — Day 5
## Bin Packing — All Three Strategies

---

## Pre-Session Checklist

- [ ] Read `MEMORY.md` — confirm Day 4 decisions
- [ ] Read `PROGRESS.md` — Day 4 complete, Day 5 section
- [ ] Confirm all tests pass: `mvn test`
- [ ] Confirm branch: `day/05-bin-packing-strategies`
  ```bash
  git checkout day/05-bin-packing-strategies
  ```

---

## Context

**Plan reference:** Section 3 (Algorithm Design — Bin Packing),
Section 4 (Event Types — VEHICLE_BREAKDOWN),
Section 5 (FR-ALG-05 — all 3 strategies mandatory),
Section 16 (Metrics — bin packing comparison)

**Goal:** Implement all three bin packing strategies as swappable
`PackingStrategy` implementations, wire them to both capacity
validation (NEW_ORDER) and full redistribution (VEHICLE_BREAKDOWN),
and produce measurable approximation ratio differences between strategies.

---

## Tasks

### 1. PackingStrategy Interface
Define in `com.routepulse.algorithm.binpacking`:

```java
public interface PackingStrategy {
    BinPackingResult pack(List<CargoItem> items,
                          Map<CourierId, CargoCapacity> availableCapacity);
    String strategyName();
}
```

```java
public record BinPackingResult(
    Map<CourierId, List<CargoItem>> assignment,
    Map<CourierId, Double> wastedCapacityPercent,
    double approximationRatio,
    int couriersUsed,
    List<String> unassignedOrderIds,
    long executionTimeMs
) {}
```

Approximation ratio formula:
```
ratio = couriersUsed / ceil(totalCargoWeight / maxCapacityPerCourier)
```
A ratio of 1.0 is optimal. FFD guarantee: â‰¤ (11/9) â‰ˆ 1.22.

### 2. FirstFitStrategy
```
For each item (in input order):
  Find first courier with remaining capacity â‰¥ item size
  Assign item to that courier
  If no courier fits -> item goes to unassigned list
```

### 3. BestFitStrategy
```
For each item (in input order):
  Find courier where (remaining_capacity - item_size) is minimized
  but still â‰¥ 0 (tightest fit that works)
  Assign item to that courier
  If no courier fits -> item goes to unassigned list
```

### 4. FirstFitDecreasingStrategy
```
1. Sort items by cargo weight DESCENDING (large items first)
2. Apply First Fit algorithm on the sorted list
```

The pre-sort is the key insight. Add a JavaDoc comment explaining
why this produces better packing: large items are placed first,
leaving small gaps for small items rather than large items not fitting.

### 5. BinPackingModule
Implement `com.routepulse.algorithm.binpacking.BinPackingModule`
implementing `AlgorithmModule<BinPackingInput, BinPackingOutput>`.

**Always runs all 3 strategies on every call:**

```java
public BinPackingOutput solve(BinPackingInput input) {
    var ffResult = firstFit.pack(input.items(), input.capacity());
    var bfResult = bestFit.pack(input.items(), input.capacity());
    var ffdResult = firstFitDecreasing.pack(input.items(), input.capacity());

    return new BinPackingOutput(ffResult, bfResult, ffdResult,
                                input.operationType());
}
```

The `operationType` flag distinguishes capacity validation
(NEW_ORDER) from full redistribution (VEHICLE_BREAKDOWN).

For **capacity validation** (NEW_ORDER):
- Run all 3 for metrics logging
- Use FFD result for the actual capacity decision
- If FFD says order fits -> proceed; if not -> trigger redistribution

For **redistribution** (VEHICLE_BREAKDOWN):
- Run all 3 for the comparison panel
- Use FFD result for actual cargo assignment
- Log all 3 results to `MetricsStore` for the comparison report

### 6. Wire VEHICLE_BREAKDOWN Handler
In `EventDispatcher`, implement `handleVehicleBreakdown(...)`:

```
1. Deactivate courier via CourierRegistry
2. Extract all undelivered orders from deactivated courier's manifest
3. Build redistribution input:
   - Items: undelivered orders as CargoItems
   - Available capacity: remaining capacity of all active couriers
4. Call BinPackingModule.solve(...)
5. For each order in FFD assignment:
   a. Call GreedyInsertionModule.solve() to place it in receiving courier's route
   b. Update OrderRegistry (still ASSIGNED, just new courier)
6. Return composite mutation covering all assignments
```

### 7. Replace NEW_ORDER Stub
Remove the stub capacity validation added on day 4.
Replace with a real call to `BinPackingModule.solve()` with
`operationType = CAPACITY_VALIDATION`.

### 8. Cargo Size Distribution
Ensure the order generator (in `ScenarioLoader`) produces
a bimodal cargo size distribution:
- 30% of orders: weight = 0.60–0.70 of courier capacity
- 70% of orders: weight = 0.15–0.20 of courier capacity

This is required for FF vs BF vs FFD to produce measurably
different approximation ratios. Document this in `ScenarioLoader`
with a comment explaining the adversarial distribution choice.

---

## Unit Tests

File: `BinPackingModuleTest.java`

**Test 1 — FFD correct assignment on bimodal distribution:**
4 couriers, 8 orders (bimodal sizes). Run FFD.
Assert: no courier exceeds capacity.
Assert: approximation ratio â‰¤ 1.23 (within FFD guarantee).

**Test 2 — Over-capacity scenario — no violation:**
Create a scenario where items cannot all fit in available couriers.
Assert: no assignment in any strategy exceeds capacity.
Assert: unassigned orders list is non-empty.

**Test 3 — FFD â‰¤ FF on approximation ratio:**
Run all 3 strategies on the same bimodal input.
Assert: `ffd.approximationRatio() â‰¤ ff.approximationRatio()`.

**Test 4 — Determinism:**
Run all 3 strategies twice on identical input.
Assert: results are identical both times.

---

## Definition of Done

- [ ] `mvn test` — all 4 bin packing tests pass
- [ ] All 3 strategies implement `PackingStrategy` interface
- [ ] `BinPackingModule` always runs all 3 strategies
- [ ] FFD result used for actual cargo decisions
- [ ] All 3 results logged to `MetricsStore` on redistribution event
- [ ] `VEHICLE_BREAKDOWN` handler fully wired
- [ ] Day 4 NEW_ORDER stub replaced with real capacity check
- [ ] Bimodal cargo distribution in scenario generator
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` Day 5 checklist completed

---

## What NOT to Do Today

- Do not implement DP — that is day 6
- Do not implement frontend components
- Do not change the greedy insertion module from day 4

---

## Post-Session

```bash
git add .
git commit -m "day 5: bin packing FF/BF/FFD, vehicle breakdown handler, 4 tests"
git push origin day/05-bin-packing-strategies
git checkout main
git merge day/05-bin-packing-strategies
```

Update `MEMORY.md` and `PROGRESS.md` before closing.

