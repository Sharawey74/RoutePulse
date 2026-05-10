# SESSION — Day 10
## Scenarios + Polish + Delivery

---

## Pre-Session Checklist

- [ ] Read `MEMORY.md` — confirm Day 9 decisions
- [ ] Read `PROGRESS.md` — Day 9 complete, Day 10 section
- [ ] Full system test: `docker compose up` — both services healthy
- [ ] Full 100-tick demo run — all panels populate correctly
- [ ] Confirm branch: `day/10-scenarios-polish-delivery`
  ```bash
  git checkout phase/5-polish
  git checkout -b day/10-scenarios-polish-delivery
  ```

---

## Context

**Plan reference:** Section 19 (Optional Features — algorithm toggle,
scenario designer), Section 15 (Deployment), Section 17 (Testing —
determinism test), Section 20 (Roadmap — Phase 5 Polish)

**Goal:** Implement two scripted demonstration scenarios,
add the algorithm strategy toggle, run the determinism test,
finalize Docker deployment, write the README and algorithm
comparison report, and deliver a clean demo-ready artifact.

**This day is also the buffer.** If any day 6, 7, or 8 item
is incomplete, resolve it before adding new features.
Do not add scope. Close existing gaps first.

---

## Tasks

### 1. Scripted Demonstration Scenarios

Both scenarios are JSON files in
`backend/src/main/resources/scenarios/`.

---

**Scenario 1: `rush_hour.json` — "Rush Hour Congestion"**

Purpose: Demonstrate repeated Dijkstra re-runs and greedy insertion
under high-order-volume conditions. DP fires during a brief quiet
window and produces a visible step-down on the quality chart.

Event schedule (key events):
```json
{
  "name": "rush_hour",
  "graph": "demo_graph",
  "couriers": 6,
  "initialOrders": 12,
  "events": [
    { "tick": 5,  "type": "NEW_ORDER", ... },
    { "tick": 8,  "type": "NEW_ORDER", ... },
    { "tick": 10, "type": "ROAD_WEIGHT_CHANGE", "from": 3, "to": 7, "newWeight": 18 },
    { "tick": 12, "type": "NEW_ORDER", ... },
    { "tick": 14, "type": "ROAD_WEIGHT_CHANGE", "from": 11, "to": 15, "newWeight": 16 },
    { "tick": 18, "type": "NEW_ORDER", ... },
    { "tick": 22, "type": "NEW_ORDER", ... },
    { "tick": 25, "type": "NEW_ORDER", ... },
    { "tick": 30, "type": "NEW_ORDER", ... },
    // Quiet window ticks 33–40 → DP fires → step-down on chart
    { "tick": 42, "type": "NEW_ORDER", ... },
    { "tick": 45, "type": "ROAD_WEIGHT_CHANGE", "from": 8, "to": 12, "newWeight": 19 },
    { "tick": 50, "type": "NEW_ORDER", ... },
    // ... remaining orders arriving through tick 80
  ]
}
```

**Design requirement:** Orders must arrive alternating between
District A and District B nodes. This is what forces the greedy
cross-cluster insertions that DP corrects.

---

**Scenario 2: `road_closure.json` — "Road Closure Mid-Delivery"**

Purpose: Demonstrate Dijkstra re-routing after edge removal,
followed by vehicle breakdown redistribution using bin packing,
followed by DP re-optimization during the quiet recovery period.

Event schedule (key events):
```json
{
  "name": "road_closure",
  "graph": "demo_graph",
  "couriers": 5,
  "initialOrders": 15,
  "events": [
    { "tick": 5,  "type": "NEW_ORDER", ... },
    { "tick": 10, "type": "NEW_ORDER", ... },
    { "tick": 15, "type": "ROAD_REMOVAL",          // Main artery closure
      "from": 6, "to": 10 },
    { "tick": 16, "type": "ROAD_REMOVAL",          // Second route cut off
      "from": 7, "to": 11 },
    { "tick": 20, "type": "NEW_ORDER", ... },
    { "tick": 28, "type": "VEHICLE_BREAKDOWN",     // Courier 2 breakdown
      "courierId": "C2" },
    { "tick": 30, "type": "NEW_ORDER", ... },
    { "tick": 35, "type": "PRIORITY_ESCALATION",   // Escalate order from breakdown
      "orderId": "O-007" },
    // Quiet window ticks 42–50 → DP fires for redistribution-affected couriers
    { "tick": 55, "type": "NEW_ORDER", ... },
    // ... remaining orders through tick 85
  ]
}
```

**Design requirement:** The two road removals must affect at least
two different active couriers' routes. The vehicle breakdown must
occur while Courier 2 has exactly 5 remaining stops — this triggers
the brute-force verifier on redistribution, visible in the DP table.

---

**Scenario Verification Checklist:**
- [ ] Rush Hour: DP fires at least once, quality chart shows step-down
- [ ] Rush Hour: Quality gap ≥ 10% by tick 60 (verify in report)
- [ ] Road Closure: Dijkstra re-runs logged for ≥ 2 couriers on closure event
- [ ] Road Closure: BinPacking FFD handles breakdown redistribution
- [ ] Road Closure: DP fires during quiet window after breakdown
- [ ] Both scenarios reach tick 100 without errors

---

### 2. Algorithm Strategy Toggle

Add to `ControlPanel` a toggle with three modes:

```typescript
type AlgorithmMode = 'adaptive' | 'greedyOnly' | 'dpOnly'
```

Wire via `POST /api/simulation/config` with body `{ algorithmMode }`.

**Backend — `SimulationConfig` update:**
Add `algorithmMode: AlgorithmMode` field.

**Backend — `EventDispatcher` change** (one flag, not algorithm restructure):
```java
// In handleQuietPeriod():
if (config.algorithmMode() == GREEDY_ONLY) {
    return new NoOpMutation();   // DP suppressed
}

// In handleNewOrder():
if (config.algorithmMode() == DP_ONLY) {
    // Skip greedy, run DP immediately for this courier
    // (Run greedy to get a valid insertion, then immediately DP-optimize)
}
```

The strategy toggle must NOT change any algorithm module code —
only the dispatcher logic changes. This demonstrates the
Open/Closed Principle: new behavior via configuration, not
modification of existing algorithm modules.

**Mode comparison panel** (in `GreedyVsDPPanel`):
When switching modes mid-simulation, the panel notes the mode
boundary in the quality chart with a vertical dashed line.

---

### 3. Manual Event Injection Form

Complete the manual event injection UI in `ControlPanel`:

```typescript
// Form fields vary by event type
const ManualEventForm = () => {
  const [eventType, setEventType] = useState<EventType>('NEW_ORDER')
  // Render different fields per type:
  // NEW_ORDER: node selector (dropdown of graph nodes)
  // ROAD_WEIGHT_CHANGE: from/to node selectors + weight slider (1–20)
  // ROAD_REMOVAL: from/to node selectors
  // VEHICLE_BREAKDOWN: courier selector
  // PRIORITY_ESCALATION: order ID input
}
```

Wire to `POST /api/events/inject`.
This is the operator's primary demo interaction tool.

---

### 4. Determinism Test

File: `SimulationDeterminismTest.java`

```java
@Test
void sameScenarioProducesIdenticalOutput() {
    // Run 1
    engine.init("rush_hour");
    engine.run(100);
    ShiftReport report1 = reportGenerator.generate();
    List<EventLogEntry> log1 = eventLogStore.getAll();

    engine.reset();

    // Run 2
    engine.init("rush_hour");
    engine.run(100);
    ShiftReport report2 = reportGenerator.generate();
    List<EventLogEntry> log2 = eventLogStore.getAll();

    // Assert identical output
    assertThat(report1.summary().totalOrdersDelivered())
        .isEqualTo(report2.summary().totalOrdersDelivered());
    assertThat(report1.summary().totalFleetDistance())
        .isCloseTo(report2.summary().totalFleetDistance(), within(0.001));
    assertThat(log1).hasSameSizeAs(log2);
    assertThat(log1.get(0).tick()).isEqualTo(log2.get(0).tick());
}
```

---

### 5. Docker Compose — Final Verification

`docker-compose.yml` (verify all fields are correct):
```yaml
version: '3.9'
services:
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    volumes:
      - ./reports:/app/reports
    environment:
      - SIMULATION_MAX_TICKS=100
      - SIMULATION_DP_STOP_CAP=10
      - SIMULATION_QUIET_PERIOD_TICKS=5
      - SIMULATION_DP_BUDGET_MS=200

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    depends_on:
      - backend
```

`Makefile`:
```makefile
build:
	docker compose build

up:
	docker compose up -d

down:
	docker compose down

test:
	cd backend && mvn test
	cd frontend && npm test -- --run

logs:
	docker compose logs -f backend

report:
	open reports/ || xdg-open reports/
```

**Verify:**
```bash
make build   # both images build cleanly
make up      # both containers healthy
make test    # all tests pass
```

---

### 6. README.md (Project Root)

Sections required:
```
## AI-Based Delivery Optimizer

### What This Is
### What This Is Not
### Quick Start (3 commands)
### Scenarios
### Algorithm Overview
  - Dijkstra (with correct complexity)
  - Greedy Insertion (correct O(k×m) complexity)
  - DP Re-Optimizer (Held-Karp, stop cap, time budget)
  - Bin Packing (FF vs BF vs FFD, approximation ratios)
### Architecture
### Scale Parameters
### Configuration
### Running Tests
### Deployment
```

Keep it concise. Max 300 lines. Link to the algorithm
comparison report for technical depth.

---

### 7. Algorithm Comparison Report

File: `docs/algorithm_comparison_report.md`

Required sections (this is the course deliverable):

```
## 1. Problem Statement
## 2. Algorithm Descriptions
   ### 2.1 Dijkstra — Full Single-Source Re-Run
        - Variant chosen and why
        - Complexity analysis (correct)
        - Why incremental was excluded
   ### 2.2 Greedy Cheapest Insertion
        - Correct O(k×m) complexity with justification
        - Why O(n) claim in project guide is wrong
        - Insertion cost formula derivation
   ### 2.3 DP Route Re-Optimization (Held-Karp)
        - State definition, transition, base case
        - Stop cap justification (2^10 = 1024 states)
        - Brute-force verification methodology
   ### 2.4 Bin Packing
        - Three strategies compared
        - FFD approximation guarantee: ≤(11/9)OPT
        - Empirical results from simulation runs

## 3. Experimental Results
   ### 3.1 Greedy vs. DP Quality Gap
        - Quality gap chart (screenshot from Rush Hour scenario)
        - Measured % improvement by tick 60
        - Shadow route methodology explanation
   ### 3.2 Algorithm Execution Times
        - Measured times at committed scale
        - Complexity verified empirically
   ### 3.3 Bin Packing Strategy Comparison
        - Table: FF vs BF vs FFD on Rush Hour scenario
        - Approximation ratios achieved vs. theoretical bound

## 4. Engineering Trade-Offs
   - Why greedy for real-time, DP for optimization windows
   - Why full Dijkstra re-run (not incremental) is correct here
   - Why bimodal cargo distribution is necessary for valid comparison

## 5. Conclusion
```

---

### 8. Export One Demonstration Artifact

Run the `rush_hour` scenario to completion. Export the report:
```bash
curl -o demo_shift_report.json \
     http://localhost:8080/api/reports/shift/export
```

Commit this file to the repository in `docs/demo_results/`.
This is the evidence of a working system in the submission.

---

### 9. Final Code Quality Pass

Before final commit, run through each algorithm module:
- [ ] Every class has a one-sentence JavaDoc purpose statement
- [ ] Every algorithm has complexity stated in JavaDoc
- [ ] No magic numbers — all constants in `SimulationConfig`
- [ ] No method exceeds 30 lines (extract if needed)
- [ ] Guard clauses present in all algorithm entry methods
- [ ] No imports from higher layers in algorithm package

---

## Definition of Done

- [ ] `make test` — all tests pass (backend + frontend)
- [ ] `make up` — both containers start cleanly
- [ ] `rush_hour` scenario: quality gap ≥ 10% by tick 60 (verified)
- [ ] `road_closure` scenario: breakdown redistribution and DP both fire
- [ ] Algorithm strategy toggle works (Adaptive / Greedy Only / DP Only)
- [ ] Manual event injection form functional for all 5 event types
- [ ] Determinism test passes (2 runs produce identical output)
- [ ] README covers all required sections
- [ ] Algorithm comparison report written (course deliverable)
- [ ] `demo_shift_report.json` exported and committed
- [ ] Docker Compose runs cleanly from a fresh clone
- [ ] Phase 5 merged: `day/10` → `phase/5-polish` → `develop` → `main`
- [ ] `MEMORY.md` updated with final project state
- [ ] `PROGRESS.md` all days marked complete

---

## What NOT to Do Today

- Do not add new algorithm types
- Do not add geographic map tiles or real city data
- Do not add smooth animation or interpolation to the map
- Do not add authentication or user management
- Do not add any feature not already partially built

---

## Final Merge Protocol

```bash
git add .
git commit -m "day 10: scenarios, algorithm toggle, determinism test,
               docker finalization, readme, comparison report"

git checkout phase/5-polish
git merge day/10-scenarios-polish-delivery

git checkout develop
git merge phase/5-polish

git checkout main
git merge develop
git tag v1.0-final
git push origin main --tags
```

---

## Post-Project Verification Checklist

Run these in sequence from a clean clone on a fresh machine:

```bash
git clone <repo>
cd delivery-optimizer
make build        # must succeed with no warnings
make test         # all tests must pass
make up           # both containers must start

# Open browser: http://localhost:3000
# Select "Rush Hour Congestion" scenario
# Click "Run"
# Verify: quality chart shows step-down at DP cycle
# Verify: report modal appears at tick 100
# Verify: export downloads valid JSON

make down
```

If all steps succeed: the project is complete and deliverable.

Update `MEMORY.md` and `PROGRESS.md` to reflect final state.
