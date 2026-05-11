# SESSION — Day 2
## Simulation Engine + Event Queue

---

## Pre-Session Checklist

- [ ] Read `MEMORY.md` — confirm Day 1 decisions
- [ ] Read `PROGRESS.md` — Day 1 complete, Day 2 section
- [ ] Confirm all Day 1 tests still pass: `mvn test`
- [ ] Confirm branch: `day/02-simulation-engine-event-queue`
  ```bash
  git checkout day/02-simulation-engine-event-queue
  ```

---

## Context

**Plan reference:** Section 4 (Event-Driven Simulation Design),
Section 8 (System Design — Command Pattern, Builder Pattern),
Section 9 (Backend Design — Simulation Engine Design)

**Goal:** Build the simulation engine skeleton — the event queue,
the clock, the dispatcher structure, and the mutation applier.
No real algorithm calls yet; handlers are no-ops that log the event.
This is the backbone everything plugs into from day 3 onward.

---

## Tasks

### 1. Event Model
Define in `com.routepulse.simulation.events`:

```
EventType (enum):
  NEW_ORDER, ROAD_WEIGHT_CHANGE, ROAD_REMOVAL,
  VEHICLE_BREAKDOWN, PRIORITY_ESCALATION, QUIET_PERIOD

EventPriority (enum, ordered 1–6):
  ROAD_REMOVAL(1), ROAD_WEIGHT_CHANGE(2), VEHICLE_BREAKDOWN(3),
  NEW_ORDER(4), PRIORITY_ESCALATION(5), QUIET_PERIOD(6)

Event (class — use Builder pattern):
  scheduledTick: SimulatedTick
  type: EventType
  priority: EventPriority
  payload: EventPayload (sealed interface)
  createdAt: SimulatedTick
```

Payload classes:
```
NewOrderPayload(Order order)
RoadWeightChangePayload(NodeId from, NodeId to, int newWeight)
RoadRemovalPayload(NodeId from, NodeId to)
VehicleBreakdownPayload(CourierId courierId)
PriorityEscalationPayload(String orderId)
```

### 2. Event Queue
Implement `com.routepulse.simulation.engine.EventQueue`:
- Internal: `List<Event>` sorted by: tick ascending, then priority ascending
- `enqueue(Event)` — inserts in sorted position
- `dequeue()` — removes and returns head
- `peek()` — returns head without removing
- `isEmpty()` — boolean
- `size()` — int

**Use sorted ArrayList** (not PriorityQueue). Simple and correct at
this scale. Comment in code: "Sorted ArrayList; suitable for â‰¤200 events.
Replace with PriorityQueue if queue depth grows beyond 500."

### 3. SimClock
Implement `com.routepulse.simulation.engine.SimClock`:
- `currentTick()` -> `SimulatedTick`
- `advance()` — increments tick by 1
- `reset()` — returns to tick 0
- Immutable from outside — only simulation engine calls `advance()`

### 4. QuietPeriodMonitor
Implement `com.routepulse.simulation.engine.QuietPeriodMonitor`:
- `ticksSinceLastDisruption` counter
- `recordDisruption()` — resets counter to 0
- `tick()` — increments counter
- `isQuietPeriodReached(int threshold)` — returns boolean
- `reset()` — resets counter to 0
- Default threshold: configurable via `SimulationConfig` (default: 5)

### 5. Mutation Types
Define `com.routepulse.simulation.engine.Mutation` as a
sealed interface with initial permitted types:

```java
sealed interface Mutation permits
    NoOpMutation,
    RouteInsertionMutation,
    RouteReorderMutation,
    EdgeWeightMutation,
    EdgeRemovalMutation,
    CourierDeactivationMutation,
    CargoAssignmentMutation
```

Each type as a Java `record`.

### 6. MutationApplier
Implement `com.routepulse.simulation.engine.MutationApplier`:
- `apply(Mutation mutation, StateLayer state)` — dispatches by type
- All cases **no-op** for now (log mutation type and return)
- Uses pattern matching on sealed interface:
  ```java
  switch (mutation) {
      case NoOpMutation m -> log.debug("no-op mutation");
      case EdgeWeightMutation m -> applyEdgeWeight(m, state);
      // etc.
  }
  ```

### 7. EventDispatcher
Implement `com.routepulse.simulation.engine.EventDispatcher`:
- `dispatch(Event event)` -> `Mutation`
- One handler method per event type
- All handlers: log event type + tick, return `NoOpMutation`
- Comment each handler with the algorithm chain it will execute from day 3

### 8. SimulationEngine
Implement `com.routepulse.simulation.engine.SimulationEngine`:
- Dependencies (injected via constructor): `EventQueue`, `SimClock`,
  `EventDispatcher`, `MutationApplier`, `QuietPeriodMonitor`
- `step()`:
  1. If queue empty -> return current state snapshot (no-op)
  2. Dequeue next event
  3. `dispatcher.dispatch(event)` -> mutation
  4. `applier.apply(mutation, state)`
  5. `monitor.recordDisruption()` if event is not QUIET_PERIOD
  6. `monitor.tick()`
  7. If `monitor.isQuietPeriodReached(threshold)` -> enqueue QUIET_PERIOD
  8. `clock.advance()`
  9. Return state snapshot
- `run(int ticks)` — calls `step()` n times
- `reset()` — reinitializes all state, reloads scenario

### 9. ScenarioLoader
Implement `com.routepulse.simulation.scenario.ScenarioLoader`:
- `load(String scenarioName)` -> `ScenarioDefinition`
- Reads from `src/main/resources/scenarios/{name}.json`
- `ScenarioDefinition`: graph config, courier config, order schedule
  (tick + order payload), disruption event schedule

---

## Verification

Run a manual test (no JUnit yet — verified by logging):
1. Create a 10-event queue with mixed types and ticks
2. Dequeue all events — verify order: by tick, then by priority
3. Run `step()` 10 times — verify each event is processed exactly once
4. Verify `QuietPeriodMonitor` fires after 5 consecutive quiet ticks

---

## Definition of Done

- [ ] `SimulationEngine.step()` processes events in correct order
- [ ] `QuietPeriodMonitor` fires at correct threshold
- [ ] `Mutation` sealed interface covers all required types
- [ ] `EventDispatcher` has one handler per event type (all no-ops)
- [ ] No algorithm logic exists in `SimulationEngine`
- [ ] No state mutation logic exists in `EventDispatcher`
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` Day 2 checklist completed

---

## What NOT to Do Today

- Do not wire algorithm modules — handlers are no-ops only
- Do not implement REST or SSE endpoints today
- Do not implement `StateLayer` objects — that is day 3
- Do not implement `ScenarioDefinition` JSON loading yet
  (just define the class structure)

---

## Post-Session

```bash
git add .
git commit -m "day 2: simulation engine, event queue, simclock, dispatcher skeleton"
git push origin day/02-simulation-engine-event-queue
git checkout main
git merge day/02-simulation-engine-event-queue
```

Update `MEMORY.md` and `PROGRESS.md` before closing.

