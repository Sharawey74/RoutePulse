# SESSION — Day 8
## Backend: Metrics + Report | Frontend: Map Canvas + Event Log

---

## Pre-Session Checklist

- [ ] Read `MEMORY.md` — confirm Day 7 decisions
- [ ] Read `PROGRESS.md` — Day 7 complete, Day 8 section
- [ ] Backend: `mvn test` — all tests pass
- [ ] Frontend: `npm run dev` — no console errors
- [ ] Confirm branch: `day/08-metrics-report-map-canvas`
  ```bash
  git checkout day/08-metrics-report-map-canvas
  ```

---

## Context

**Plan reference:** Section 16 (Metrics, Monitoring, Reporting),
Section 9 (Backend — Report Generation),
Section 10 (Frontend — Map Canvas, Event Log Panel)

**Goal (backend):** Complete metrics accumulation, implement the
full end-of-shift report, and expose the report endpoint.

**Goal (frontend):** Build the live delivery map on HTML Canvas
and the scrollable event log panel. Both must update after every
SSE tick.

---

## BACKEND TASKS

### 1. MetricsStore — Complete Tick Accumulation
Enhance `MetricsStore` to record per-tick snapshots:

```java
public record TickSnapshot(
    int tick,
    double totalFleetDistance,
    double avgDistancePerRemainingStop,  // fleet average
    Map<String, Double> perCourierActiveDistance,
    Map<String, Double> perCourierShadowDistance,
    Map<String, Double> perCourierQualityGap,
    int ordersDelivered, int ordersPending,
    int activeCouriers,
    boolean dpRanThisTick,
    double dpImprovementKm
) {}
```

`MetricsStore.recordTick()` is called by `SimulationEngine.step()`
after every mutation is applied, before SSE push.

### 2. Quality Gap Computation
In `SimulationEngine.step()`, after each mutation:

```java
// Compute quality gap per courier
for (CourierId id : courierRegistry.getActiveCouriers()) {
    double activeRemainingDist = routeRegistry.getRemainingDistance(id, distMatrix);
    double shadowRemainingDist = shadowRouteRegistry.getRemainingDistance(id, distMatrix);
    int remainingStops = routeRegistry.getRemainingStopCount(id);

    if (remainingStops > 0) {
        double activeDps = activeRemainingDist / remainingStops;
        double shadowDps = shadowRemainingDist / remainingStops;
        metricsStore.recordCourierQuality(id, activeDps, shadowDps);
    }
}
```

`distance_per_remaining_stop` is always the quality metric.
Never use raw total distance in the comparison.

### 3. ShiftReport Model
```java
public record ShiftReport(
    ReportMetadata metadata,
    ReportSummary summary,
    AlgorithmPerformanceSummary algorithmPerformance,
    BinPackingComparisonSummary binPackingComparison,
    List<CourierSummary> courierSummaries,
    List<VerificationLogEntry> dpVerificationLog,
    List<TickSnapshot> fullTimeSeries
) {}

public record ReportSummary(
    int totalOrdersDelivered, int totalOrders,
    double onTimeDeliveryRate,
    double totalFleetDistance,
    double totalDpDistanceSaved,
    double greedyShadowDistance,
    double dpImprovementPercent,
    int totalDisruptions,
    Map<String, Integer> disruptionBreakdown
) {}

public record AlgorithmPerformanceSummary(
    long dijkstraCallCount, double dijkstraAvgTimeMs,
    long greedyCallCount, double greedyAvgTimeMs,
      double greedyAvgPositionsEvaluated,
    long dpCallCount, double dpAvgTimeMs,
      double dpAvgImprovementKm, double dpAvgImprovementPct,
      int dpBudgetExceededCount,
    BinPackingStrategySummary ff,
    BinPackingStrategySummary bf,
    BinPackingStrategySummary ffd
) {}
```

### 4. ReportGenerator
```java
@Component
public class ReportGenerator {
    public ShiftReport generate(MetricsStore metrics,
                                EventLogStore events,
                                SimulationConfig config) {
        // Aggregate all metrics into ShiftReport
        // Compute summary statistics from event log
        // Return complete report
    }
}
```

### 5. Report Endpoints
```java
@GetMapping("/api/reports/shift")
public ResponseEntity<ShiftReport> getReport() { ... }

@GetMapping("/api/reports/shift/export")
public ResponseEntity<Resource> exportReport() {
    // Returns JSON file as downloadable attachment
    // Content-Disposition: attachment; filename="shift_report_{timestamp}.json"
}
```

### 6. simulationComplete SSE Event
When `SimClock.currentTick() == maxTicks`:
- `SimulationEngine` calls `ReportGenerator.generate()`
- SSE pushes event named `simulationComplete` with full `ShiftReport` JSON
- Frontend `simulationComplete` listener triggers `ReportModal`

### 7. Phase 3 Merge (end of today)
```bash
git checkout main
git merge day/08-metrics-report-map-canvas
git tag v0.3-phase3-complete
```

---

## FRONTEND TASKS

### 1. DeliveryMapCanvas Component
File `src/components/map/DeliveryMapCanvas.tsx`:

```typescript
const DeliveryMapCanvas = () => {
  const { state } = useSimulation()
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    if (!state || !canvasRef.current) return
    const ctx = canvasRef.current.getContext('2d')!
    drawMap(ctx, state)
  }, [state])  // redraws on every state update

  return <canvas ref={canvasRef} width={700} height={500} />
}
```

**`drawMap(ctx, state)` — draw in this order:**
1. Clear canvas: `ctx.clearRect(0, 0, width, height)`
2. Draw edges — color by weight:
   - weight 1–6: green (#4CAF50)
   - weight 7–13: yellow (#FF9800)
   - weight 14–20: red (#F44336)
3. Draw route polylines — one color per courier (6-color fixed palette):
   `['#2196F3','#9C27B0','#00BCD4','#FF5722','#8BC34A','#FF4081']`
4. Draw node circles (radius 8px, filled white, black border, ID label)
5. Draw courier markers (radius 6px, solid courier color, white border)
6. Draw depot node (larger circle, black fill, "D" label)

**Node coordinates** come from the graph definition JSON.
Map x/y coordinates to canvas pixels via a simple linear transform
with 40px padding on each side.

### 2. EventLogPanel Component
File `src/components/panels/EventLogPanel.tsx`:

- Renders `state.recentEvents` (last 20 entries)
- Each entry shows: tick, event type badge, courier ID, algorithm,
  time ms, cost delta (green if negative = improvement, red if positive)
- Event type badge colors:
  - NEW_ORDER: blue
  - ROAD_REMOVAL: red
  - ROAD_WEIGHT_CHANGE: orange
  - VEHICLE_BREAKDOWN: purple
  - QUIET_PERIOD: green
  - PRIORITY_ESCALATION: yellow
- **Virtualized list** using CSS overflow-y scroll.
  Do NOT render all 200 events in DOM — only show last 20 from state.
- Auto-scrolls to bottom on new events

### 3. Main Layout
File `src/App.tsx`:

```tsx
<SimulationProvider>
  <div className="h-screen flex flex-col">
    <ControlPanel />
    <div className="flex flex-1 overflow-hidden">
      <div className="flex-[3]">
        <DeliveryMapCanvas />
      </div>
      <div className="flex-[2] overflow-y-auto">
        <EventLogPanel />
      </div>
    </div>
  </div>
</SimulationProvider>
```

### 4. Verification
Run full 100-tick scenario. Verify:
- Map redraws after every Step click
- Route polylines appear for each courier
- Edge colors change when road weight change events fire
- Event log scrolls and shows correct entries
- No dropped SSE events (verify tick counter is sequential)

---

## Definition of Done

**Backend:**
- [ ] `MetricsStore` records `distance_per_remaining_stop` per courier per tick
- [ ] `ReportGenerator` produces complete `ShiftReport` with all fields
- [ ] `GET /api/reports/shift` returns valid JSON report
- [ ] `simulationComplete` SSE event fires at tick 100 with full report
- [ ] Phase 3 merged to `main` and `main`
- [ ] `MEMORY.md` updated

**Frontend:**
- [ ] Map canvas renders graph correctly
- [ ] Edge colors update on road weight change events
- [ ] Route polylines update after each tick
- [ ] Event log shows last 20 events with correct styling
- [ ] No console errors during 100-tick run
- [ ] `MEMORY.md` updated

- [ ] `PROGRESS.md` Day 8 checklist completed

---

## Post-Session

```bash
# Backend
git add .
git commit -m "day 8 backend: metrics store, report generator, simulationComplete SSE"

# Frontend
git add .
git commit -m "day 8 frontend: map canvas, event log panel, main layout"
git checkout main
git merge day/08-metrics-report-map-canvas
```

Update `MEMORY.md` and `PROGRESS.md` before closing.

