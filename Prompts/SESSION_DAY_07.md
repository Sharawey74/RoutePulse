# SESSION — Day 7
## Backend: Full Integration + SSE | Frontend: Setup + SSE

---

## Pre-Session Checklist

- [ ] Read `MEMORY.md` — confirm Phase 2 decisions
- [ ] Read `PROGRESS.md` — Phase 2 complete, Day 7 section
- [ ] Confirm all backend tests pass: `mvn test`
- [ ] Confirm two branches for today:

**Backend:**
```bash
git checkout main
git checkout -b feature/backend-integration-metrics-day-7-8
```

**Frontend:**
```bash
git checkout main
git checkout -b feature/frontend-ui-day-7-10
```

Work can proceed in parallel if two team members are available.

---

## Context

**Plan reference:** Section 7 (Architecture — full data flow),
Section 9 (Backend Design — REST + SSE),
Section 10 (Frontend Design — SimulationProvider, SSE hook),
Section 12 (Communication Architecture — SSE over WebSocket)

**Goal (backend):** Wire all 6 event handlers in the dispatcher,
prove end-to-end event flow works with a 100-tick scenario run.
Implement SSE push so the frontend receives state after each tick.

**Goal (frontend):** Initialize the React + Vite + TypeScript project,
connect to the backend SSE stream, and verify state updates arrive
correctly in the browser.

---

## BACKEND TASKS

### 1. Complete EventDispatcher Wiring
All 6 handlers must now call real algorithm modules:

| Handler | Algorithm Chain |
|---|---|
| `NEW_ORDER` | GreedyInsertion â†’ BinPackingValidation â†’ ShadowUpdate |
| `ROAD_WEIGHT_CHANGE` | Dijkstra(affected) â†’ DistanceMatrix |
| `ROAD_REMOVAL` | Dijkstra(affected couriers) â†’ RouteRecheck |
| `VEHICLE_BREAKDOWN` | CourierDeactivate â†’ BinPackingFFD â†’ GreedyInsertion(each) |
| `PRIORITY_ESCALATION` | GreedyInsertion(weighted) |
| `QUIET_PERIOD` | DPReoptimiser(eligible couriers) |

**Event priority order (enforce at top of `dispatch()`):**
```java
// Road events process before order events in the same tick
// This guarantees Dijkstra updates distance matrix
// before greedy insertion reads it
```

### 2. Complete MutationApplier
Implement all mutation `apply()` cases:

```java
case RouteInsertionMutation m -> {
    routeRegistry.insertStop(m.courierId(), m.position(), m.stop());
    shadowRouteRegistry.insertStop(m.courierId(), m.position(), m.stop());
    cargoRegistry.addItem(m.courierId(), m.cargoItem());
    orderRegistry.assignOrder(m.orderId(), m.courierId());
}
case RouteReorderMutation m -> {
    routeRegistry.reorderStops(m.courierId(), m.newStopSequence());
    // Shadow NOT updated — DP improvement not reflected in shadow
}
case ShadowInsertionMutation m -> {
    shadowRouteRegistry.insertStop(m.courierId(), m.position(), m.stop());
}
case DistanceMatrixUpdateMutation m -> {
    distanceMatrix.updateRow(m.sourceIndex(), m.newDistances());
}
case CourierDeactivationMutation m -> {
    courierRegistry.deactivate(m.courierId());
    cargoRegistry.clearManifest(m.courierId());
}
case CargoAssignmentMutation m -> {
    cargoRegistry.setManifest(m.courierId(), m.items());
}
```

### 3. StateSnapshotDto — Complete Structure
```java
public record StateSnapshotDto(
    int currentTick,
    List<CourierDto> couriers,
    List<EdgeDto> edges,
    List<OrderDto> orders,
    List<EventLogEntryDto> recentEvents,   // last 20
    MetricsSummaryDto metrics
) {}

public record CourierDto(
    String id, int currentNode, String status,
    List<StopDto> remainingStops,
    double activeRouteDist, double shadowRouteDist,
    double qualityGapPercent, double cargoUtilizationPercent
) {}

public record MetricsSummaryDto(
    double totalFleetDistance, double avgDistancePerStop,
    int ordersDelivered, int ordersPending,
    int dpCyclesCompleted, double totalDpDistanceSaved
) {}
```

### 4. SSE Implementation
In `SseController`:

```java
@GetMapping(value = "/api/simulation/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream() {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    sseService.addEmitter(emitter);
    emitter.onCompletion(() -> sseService.removeEmitter(emitter));
    emitter.onTimeout(() -> sseService.removeEmitter(emitter));
    return emitter;
}
```

`SseService.push(StateSnapshotDto)`:
- Serializes to JSON
- Sends to all active emitters as event name `stateUpdate`
- On send failure â†’ remove emitter from list (client disconnected)

`SimulationEngine.step()` calls `sseService.push(snapshot)` after
each `MutationApplier.apply()` call.

### 5. Integration Tests
File: `SimulationIntegrationTest.java`

**Test 1:** Road removal + new order in same tick.
Assert: `ROAD_REMOVAL` processed before `NEW_ORDER` (verify via
event log order in `EventLogStore`).
Assert: greedy insertion uses post-Dijkstra distance matrix.

**Test 2:** Shadow route distance â‰¥ active route distance after DP.
Run a 20-tick scenario with a QUIET_PERIOD event.
After DP fires: assert shadow distance â‰¥ DP-improved distance.

### 6. Full Scenario Run
Run the pre-scripted demo scenario for 100 ticks.
Verify in logs:
- All 50 orders delivered or in-transit by tick 100
- DP fired at least once
- Bin packing ran on at least one redistribution event
- No NullPointerException or IllegalStateException in logs

---

## FRONTEND TASKS

### 1. Project Initialization
```bash
cd frontend
npm create vite@latest . -- --template react-ts
npm install
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
npm install chart.js
```

Configure `tailwind.config.js` content paths.
Add Tailwind directives to `index.css`.

### 2. TypeScript DTO Types
File `src/types/simulation.ts` — mirror all backend DTOs:

```typescript
export interface StateSnapshotDto {
  currentTick: number
  couriers: CourierDto[]
  edges: EdgeDto[]
  orders: OrderDto[]
  recentEvents: EventLogEntryDto[]
  metrics: MetricsSummaryDto
}

export interface CourierDto {
  id: string
  currentNode: number
  status: string
  remainingStops: StopDto[]
  activeRouteDist: number
  shadowRouteDist: number
  qualityGapPercent: number
  cargoUtilizationPercent: number
}
// ... all other DTOs
```

### 3. SimulationProvider
File `src/context/SimulationContext.tsx`:

```typescript
const SimulationProvider = ({ children }) => {
  const [state, setState] = useState<StateSnapshotDto | null>(null)
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    const es = new EventSource('http://localhost:8080/api/simulation/stream')

    es.addEventListener('stateUpdate', (e: MessageEvent) => {
      setState(JSON.parse(e.data))
    })

    es.addEventListener('simulationComplete', (e: MessageEvent) => {
      setState(prev => prev ? { ...prev, report: JSON.parse(e.data) } : prev)
    })

    es.onopen = () => setConnected(true)
    es.onerror = () => setConnected(false)

    return () => es.close()
  }, [])

  return (
    <SimulationContext.Provider value={{ state, connected }}>
      {children}
    </SimulationContext.Provider>
  )
}
```

### 4. useSimulation Hook
File `src/hooks/useSimulation.ts`:

```typescript
export const useSimulation = () => {
  const context = useContext(SimulationContext)
  if (!context) throw new Error('useSimulation must be inside SimulationProvider')
  return context
}
```

### 5. ControlPanel Component
File `src/components/ControlPanel.tsx`:
- Step button: `POST /api/simulation/step`
- Run button: `POST /api/simulation/run`
- Pause button: `POST /api/simulation/pause`
- Reset button: `POST /api/simulation/reset`
- Connection status indicator (green dot if `connected`)
- Current tick display

### 6. Verification
Open browser. Click "Step" 5 times. Verify:
- Browser console shows state updates arriving from SSE
- `currentTick` increments in the UI
- No CORS errors (configure Spring Boot CORS for localhost:5173)

---

## Definition of Done

**Backend:**
- [ ] All 6 event handlers fully wired with real algorithm calls
- [ ] Full 100-tick scenario completes without errors
- [ ] SSE pushes state snapshot after each tick
- [ ] 2 integration tests pass
- [ ] `MEMORY.md` updated

**Frontend:**
- [ ] Vite dev server starts: `npm run dev`
- [ ] SSE connection established with backend
- [ ] `stateUpdate` events update React state
- [ ] Step/Run/Reset buttons call correct endpoints
- [ ] No console errors
- [ ] `MEMORY.md` updated

- [ ] `PROGRESS.md` Day 7 checklist completed

---

## Post-Session

```bash
# Backend
git add .
git commit -m "day 7 backend: full dispatcher wiring, SSE, integration tests"
git checkout main
git merge feature/backend-integration-metrics-day-7-8

# Frontend
git add .
git commit -m "day 7 frontend: vite setup, sse provider, control panel"
git checkout main
git merge feature/frontend-ui-day-7-10
```

Update `MEMORY.md` and `PROGRESS.md` before closing.

