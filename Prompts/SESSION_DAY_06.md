# SESSION — Day 6
## DP Re-Optimizer + Brute-Force Verifier

---

## Pre-Session Checklist

- [ ] Read `MEMORY.md` — confirm Days 4–5 decisions
- [ ] Read `PROGRESS.md` — Day 5 complete, Day 6 section
- [ ] Confirm all tests pass: `mvn test`
- [ ] Confirm branch: `day/06-dp-reoptimiser-brute-force`
  ```bash
  git checkout day/06-dp-reoptimiser-brute-force
  ```

---

## Context

**Plan reference:** Section 3 (Algorithm Design — DP Re-Optimization),
Section 4 (Event Types — QUIET_PERIOD),
Section 5 (FR-ALG-03, FR-ALG-04 — DP + brute-force mandatory),
Section 17 (Testing Strategy — brute-force DP verification)

**Goal:** Implement the held-karp DP re-optimizer with the time budget
mechanism, implement the brute-force verifier, prove correctness before
integration, then wire the QUIET_PERIOD event. The brute-force verifier
must pass on all test cases before DP is connected to the simulation.

---

## Tasks

### 1. DP Input/Output
```java
public record DPInput(
    CourierId courierId,
    NodeId currentPosition,
    List<NodeId> remainingStops,
    double[][] distanceMatrix,
    int timeBudgetMs
) implements AlgorithmInput {}

public sealed interface DPOutput extends AlgorithmOutput
    permits DPOutput.Optimised, DPOutput.Deferred, DPOutput.BudgetExceeded {

    record Optimised(List<NodeId> optimalOrdering,
                     double totalDistance,
                     MetricsRecord metrics) implements DPOutput {}

    record Deferred(String reason) implements DPOutput {}

    record BudgetExceeded(List<NodeId> bestSoFar,
                          double bestDistance,
                          MetricsRecord metrics) implements DPOutput {}

    static DPOutput deferred(String reason) { return new Deferred(reason); }
}
```

### 2. DPReoptimiserModule — Held-Karp Implementation
Implement `com.routepulse.algorithm.dp.DPReoptimiserModule`.

**Guard clauses first:**
```java
public DPOutput solve(DPInput input) {
    int n = input.remainingStops().size();

    if (n > MAX_DP_STOPS) {
        return DPOutput.deferred("stop cap exceeded: " + n + " > " + MAX_DP_STOPS);
    }
    if (n < MIN_DP_STOPS) {
        return DPOutput.deferred("insufficient stops: " + n);
    }
    // main computation below
}
```

**State array:**
```
double[][] dp = new double[1 << n][n];
int[][] predecessor = new int[1 << n][n];
// Initialize all to Double.MAX_VALUE / 2 (avoid overflow in additions)
```

**Base case (S = single stop {s}):**
```
dp[1 << s][s] = dist[currentPositionIndex][s]
predecessor[1 << s][s] = -1  // started from current position
```

**Fill table by increasing |S|:**
```
for subsetSize from 2 to n:
  for each subset S of size subsetSize:
    for each v in S (last stop):
      for each u in S where u != v:
        S_without_v = S & ~(1 << v)
        candidate = dp[S_without_v][u] + dist[u][v]
        if candidate < dp[S][v]:
          dp[S][v] = candidate
          predecessor[S][v] = u
```

**Time budget check** (between subsetSize iterations, not inside loops):
```java
if (elapsedMs() > input.timeBudgetMs()) {
    // return best complete solution from last completed row
    return buildBudgetExceededOutput(dp, predecessor, n, bestCompleteSoFar);
}
```

**Solution extraction:**
```
fullSet = (1 << n) - 1
Find v minimizing dp[fullSet][v] + dist[v][depotIndex]
Backtrack predecessor chain to recover ordering
```

**Complexity:** O(2^n × n²). At n=10: 102,400 operations.
State this in the JavaDoc.

### 3. BruteForceVerifier
Implement `com.routepulse.algorithm.dp.BruteForceVerifier`:

```java
public VerificationResult verify(DPInput input) {
    int n = input.remainingStops().size();

    if (n > BRUTE_FORCE_CAP) {
        return VerificationResult.skipped("n=" + n + " exceeds cap");
    }

    List<NodeId> stops = input.remainingStops();
    double minDistance = Double.MAX_VALUE;
    List<NodeId> bestOrdering = null;
    int permutationCount = 0;

    for (List<NodeId> permutation : generateAllPermutations(stops)) {
        double dist = computeRouteDistance(
            input.currentPosition(), permutation, input.distanceMatrix());
        if (dist < minDistance) {
            minDistance = dist;
            bestOrdering = permutation;
        }
        permutationCount++;
    }

    return new VerificationResult(bestOrdering, minDistance, permutationCount);
}
```

```java
public record VerificationResult(
    List<NodeId> optimalOrdering,
    double optimalDistance,
    int permutationsEvaluated,
    boolean skipped,
    String skipReason
) {}
```

### 4. Verification Agreement Check
Used in tests and in simulation verbose mode:

```java
public boolean verifiesAgainstDP(DPOutput.Optimised dpResult,
                                  VerificationResult bfResult) {
    double tolerance = 0.001;
    return Math.abs(dpResult.totalDistance() - bfResult.optimalDistance()) < tolerance;
}
```

If verification fails -> log `ERROR` with both values and the input state.
A failing verification in a test is a bug in the DP implementation.

### 5. Wire QUIET_PERIOD Handler
In `EventDispatcher`, implement `handleQuietPeriod()`:

```
1. Get all active couriers from CourierRegistry
2. Filter: remainingStops.size() >= 3 AND <= MAX_DP_STOPS
3. Sort by (shadowDistance - activeDistance) descending (best improvement candidate first)
4. For each eligible courier:
   a. Build DPInput from current route state
   b. Call DPReoptimiserModule.solve(input)
   c. If Optimised and improvement > MIN_IMPROVEMENT_THRESHOLD:
      - Return RouteReorderMutation
      - Log improvement to MetricsStore
   d. Do NOT update shadow route
5. Return composite mutation for all improved couriers
```

`MIN_IMPROVEMENT_THRESHOLD` = configurable in `SimulationConfig`
(default: 0.5 km — avoids updating routes for trivial improvements).

---

## Unit Tests

File: `DPReoptimiserModuleTest.java`

**Test 1 — Correctness n=4 (DP matches brute-force):**
Build a courier with 4 remaining stops and known distance matrix.
Run both DP and brute-force verifier.
Assert `verifiesAgainstDP()` returns true.

**Test 2 — Correctness n=5 (DP matches brute-force):**
Same as Test 1 with 5 stops.
Assert agreement within 0.001.

**Test 3 — Correctness n=3 (3 distinct configurations):**
Run on 3 different known-answer inputs.
Assert all 3 agree with brute-force.

**Test 4 — Performance n=10 (completes within budget):**
Build a courier with 10 stops.
Assert `solve()` completes within 200ms.
Assert result is `DPOutput.Optimised` (not BudgetExceeded).

**Test 5 — Cap enforcement n=11:**
Build a courier with 11 stops.
Assert result is `DPOutput.Deferred`.
Assert no computation occurred (verify via metrics: statesEvaluated = 0).

---

## Definition of Done

- [ ] `mvn test` — all 5 DP tests pass
- [ ] DP matches brute-force on all 3 correctness tests (Tests 1–3)
- [ ] n=10 completes within 200ms on mainment machine (measured)
- [ ] n=11 returns `Deferred` without computation
- [ ] `QUIET_PERIOD` handler wired — triggers DP for eligible couriers
- [ ] Shadow route never updated by DP (verified by test)
- [ ] All DP calls logged to `EventLogStore` with execution time
- [ ] Phase 2 merged: `main` -> `main` -> `main`
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` Day 6 checklist completed

---

## What NOT to Do Today

- Do not implement frontend
- Do not modify the greedy insertion or bin packing modules
- Do not implement DP for more than one courier simultaneously
  (one courier per QUIET_PERIOD trigger is correct)

---

## Phase 2 Merge Protocol (End of Day 6)

```bash
git add .
git commit -m "day 6: held-karp dp, brute-force verifier, quiet period handler, 5 tests"
git push origin day/06-dp-reoptimiser-brute-force
git checkout main
git merge day/06-dp-reoptimiser-brute-force
git tag v0.2-phase2-complete
```

Update `MEMORY.md` and `PROGRESS.md` before closing.

