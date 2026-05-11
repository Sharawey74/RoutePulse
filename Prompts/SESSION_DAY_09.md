# SESSION — Day 9
## Frontend: Charts + Metrics Panels + Report Modal

---

## Pre-Session Checklist

- [ ] Read `MEMORY.md` — confirm Day 8 decisions
- [ ] Read `PROGRESS.md` — Day 8 complete, Day 9 section
- [ ] Backend running: `mvn spring-boot:run` — no errors
- [ ] Frontend: `npm run dev` — map and event log working
- [ ] Confirm branch: `day/09-frontend-charts-panels`
  ```bash
  git checkout day/09-frontend-charts-panels
  ```

---

## Context

**Plan reference:** Section 10 (Frontend Design — Component Hierarchy,
Chart Design, Report Panel), Section 16 (Metrics — all tracked KPIs),
Section 5 (FR-UI-03 through FR-UI-07)

**Goal:** Build all remaining UI panels — route quality chart,
greedy vs. DP comparison, bin packing diagram, DP table viewer,
and the end-of-shift report modal. By end of today the UI is
feature-complete and visually demonstrates algorithm behavior.

---

## Tasks

### 1. Route Quality Chart
File: `src/components/charts/RouteQualityChart.tsx`

**What it shows:**
Two line series per courier — active route quality and shadow route quality,
both measured as `distance_per_remaining_stop`. The gap between them is the
genuine DP improvement, isolated from natural route shortening.

**Implementation:**

```typescript
const RouteQualityChart = () => {
  const { state } = useSimulation()
  const chartRef = useRef<HTMLCanvasElement>(null)
  const chartInstance = useRef<Chart | null>(null)

  useEffect(() => {
    if (!chartRef.current) return
    chartInstance.current = new Chart(chartRef.current, {
      type: 'line',
      data: {
        labels: [],         // tick numbers
        datasets: []        // one active + one shadow per courier
      },
      options: {
        animation: false,   // CRITICAL: no animation for streaming data
        scales: {
          x: { title: { display: true, text: 'Simulation Tick' }},
          y: { title: { display: true, text: 'Distance per Remaining Stop (km)'}}
        }
      }
    })
  }, [])

  useEffect(() => {
    if (!state || !chartInstance.current) return
    const chart = chartInstance.current

    // Push new tick label
    chart.data.labels!.push(state.currentTick)

    // For each courier: push active and shadow data points
    state.couriers.forEach((courier, i) => {
      const activeDataset = ensureDataset(chart, `${courier.id} (active)`,
                                          COURIER_COLORS[i], false)
      const shadowDataset = ensureDataset(chart, `${courier.id} (shadow)`,
                                          COURIER_COLORS[i], true)
      activeDataset.data.push(
        courier.remainingStops.length > 0
          ? courier.activeRouteDist / courier.remainingStops.length
          : 0
      )
      shadowDataset.data.push(
        courier.remainingStops.length > 0
          ? courier.shadowRouteDist / courier.remainingStops.length
          : 0
      )
    })

    chart.update('none')   // 'none' = data update without animation
  }, [state])

  return <canvas ref={chartRef} />
}
```

Shadow series must use **dashed line style**:
```typescript
// Shadow dataset config
borderDash: [5, 5],
borderWidth: 1,
pointRadius: 0   // no dots for shadow — less visual noise
```

Active series: solid line, `borderWidth: 2`, `pointRadius: 2`.

**`ensureDataset` helper:**
Returns existing dataset if found by label, or creates and pushes a
new one. Prevents duplicate datasets on re-render.

---

### 2. Greedy vs. DP Comparison Panel
File: `src/components/panels/GreedyVsDPPanel.tsx`

**Four summary cards (top row):**
```
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ Total Saved     │ │ DP Cycles       │ │ Avg Improvement │ │ Avg DP Time     │
│  X.XX km        │ │    N            │ │   X.XX%         │ │   X ms          │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └─────────────────┘
```

Values sourced from `state.metrics`:
- `totalDpDistanceSaved`
- `dpCyclesCompleted`
- Computed: `totalDpDistanceSaved / greedyShadowDistance * 100`
- Average from event log: filter `algorithmName == "DP"`, average `executionTimeMs`

**Per-courier table (bottom):**

| Courier | Active Route | Shadow Route | Gap % | DP Cycles |
|---|---|---|---|---|
| C1 | 12.3 km | 15.1 km | 18.5% | 3 |

Color-code `Gap %`: green if > 5%, yellow if 1–5%, gray if < 1%.

---

### 3. Execution Time Chart
File: `src/components/charts/ExecutionTimeChart.tsx`

Bar chart showing average execution time per algorithm across the shift.

```typescript
// Static bar chart — updates at end of simulation only
// X-axis: algorithm names (Dijkstra, Greedy, DP, BinPacking-FF, BinPacking-BF, BinPacking-FFD)
// Y-axis: average execution time (ms)
// Data sourced from ShiftReport.algorithmPerformance when simulationComplete fires
```

Use `chart.update()` (with animation) only when `simulationComplete` arrives —
this is not a streaming chart.

---

### 4. Bin Packing Panel
File: `src/components/panels/BinPackingPanel.tsx`

**Two sections:**

**Section A — Courier Cargo Display** (live, updates each tick):
Per courier: stacked horizontal bar showing cargo slots.
Each slot: filled portion (order weight, colored by order ID),
gray portion (remaining capacity).

```typescript
// Tailwind utility bar approach — no Canvas needed here
const CourierCargoBar = ({ courier, cargo }) => (
  <div className="flex h-6 w-full rounded overflow-hidden">
    {cargo.map(item => (
      <div
        key={item.orderId}
        style={{ width: `${item.fillPercent}%` }}
        className="bg-blue-400 border-r border-white"
        title={`Order ${item.orderId}: ${item.weight}kg`}
      />
    ))}
    <div className="flex-1 bg-gray-200" title="Remaining capacity" />
  </div>
)
```

**Section B — Strategy Comparison Table** (shows after redistribution events):
Populated from `BinPackingOutput` in the event log.
Only shown when at least one `VEHICLE_BREAKDOWN` event has occurred.

| Strategy | Couriers Used | Wasted Capacity | Approx Ratio |
|---|---|---|---|
| First Fit | 5 | 18.3% | 1.17 |
| Best Fit | 5 | 14.1% | 1.17 |
| FFD | 4 | 9.2% | 1.00 |

Color rows: FFD row highlighted green (best result).
Add footnote: "FFD guarantee: ≤ (11/9)OPT ≈ 1.22"

---

### 5. DP Table Viewer
File: `src/components/panels/DPTableViewer.tsx`

**On-demand display** — not live streaming.
Triggered by clicking a courier in the route quality chart legend.

```typescript
// State: selectedCourierId | null
// When selected and DP ran for that courier this tick:
//   - Backend returns dp[S][v] table via GET /api/simulation/dp-table/{courierId}
//   - Render as a grid
```

**Add backend endpoint:**
```java
@GetMapping("/api/simulation/dp-table/{courierId}")
public ResponseEntity<DPTableDto> getDPTable(@PathVariable String courierId) {
    // Returns last computed dp[S][v] table for this courier
    // Stored in DPReoptimiserModule after last solve() call
}
```

**Frontend grid rendering:**
- Rows: subsets S (represented as binary strings, e.g., "1010")
- Columns: last node v (stop index)
- Cell value: `dp[S][v]` distance (formatted to 1 decimal)
- Highlight the optimal path cells (backtracked from solution)
- If no DP has run for selected courier: show "No DP data yet"

This panel is shown in a collapsible section below the charts,
not a modal. Keep it compact — max 10 rows visible, scrollable.

---

### 6. Report Modal
File: `src/components/ReportModal.tsx`

Triggered by the `simulationComplete` SSE event.
Full-screen overlay with semi-transparent backdrop.

**Structure:**
```
┌─────────────────────────────────────────────────────┐
│  End-of-Shift Report             [Export JSON] [×]  │
├─────────────────────────────────────────────────────┤
│  SUMMARY ROW                                         │
│  [Orders: 48/50] [On-Time: 91.7%] [Fleet Dist: Xkm] │
│  [DP Saved: 2.8km] [Disruptions: 12]                │
├─────────────────────────────────────────────────────┤
│  LEFT PANEL              │  RIGHT PANEL              │
│  Algorithm Performance   │  Bin Packing Comparison   │
│  ┌──────────────────┐   │  (strategy comparison tbl) │
│  │ Algorithm │ Calls │   │                           │
│  │ Dijkstra  │  8   │   │  DP Verification Log       │
│  │ Greedy    │ 32   │   │  n=4: DP=14.2 BF=14.2 ✓   │
│  │ DP        │  6   │   │  n=5: DP=18.7 BF=18.7 ✓   │
│  └──────────────────┘   │                           │
├─────────────────────────────────────────────────────┤
│  QUALITY CHART (final state of RouteQualityChart)   │
└─────────────────────────────────────────────────────┘
```

**Export button:**
```typescript
const handleExport = () => {
  const json = JSON.stringify(report, null, 2)
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `shift_report_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
}
```

---

### 7. Tabbed Right Panel
Wrap the right panel in a tab navigation:

```
[Event Log] [Metrics] [Bin Packing] [DP Table]
```

Active tab content renders below. Use simple state-driven tab switching —
no routing library needed.

```typescript
const [activeTab, setActiveTab] = useState<Tab>('eventLog')

const tabContent = {
  eventLog:   <EventLogPanel />,
  metrics:    <MetricsTabContent />,     // RouteQualityChart + GreedyVsDPPanel
  binPacking: <BinPackingPanel />,
  dpTable:    <DPTableViewer />
}
```

---

### 8. Scenario Selector (Placeholder)
Add to `ControlPanel`:
```typescript
<select onChange={e => initScenario(e.target.value)}>
  <option value="demo">Demo Scenario</option>
  <option value="rush_hour">Rush Hour Congestion</option>
  <option value="road_closure">Road Closure Mid-Delivery</option>
</select>
```

Wire to `POST /api/simulation/init` with the selected scenario name.
Scenario definitions are implemented on Day 10 — today just add the
selector UI and the init call.

---

## Definition of Done

- [ ] `RouteQualityChart` streams active vs. shadow lines per courier
- [ ] Shadow series renders as dashed, active as solid
- [ ] `GreedyVsDPPanel` shows 4 summary cards and per-courier table
- [ ] `ExecutionTimeChart` renders after `simulationComplete`
- [ ] `BinPackingPanel` shows cargo bars (live) and strategy table (post-breakdown)
- [ ] `DPTableViewer` renders `dp[S][v]` for selected courier
- [ ] `ReportModal` appears at tick 100 with all sections populated
- [ ] Export button downloads valid JSON
- [ ] Tabbed right panel switches between all four panels
- [ ] Scenario selector UI present in `ControlPanel`
- [ ] Phase 4 merged: `phase/4-frontend` → `develop` → `main`
- [ ] `MEMORY.md` updated
- [ ] `PROGRESS.md` Day 9 checklist completed

---

## What NOT to Do Today

- Do not implement the actual scenario JSON files — Day 10
- Do not add any new backend algorithm logic
- Do not add smooth animation to the map
- Do not add a database or any persistence layer

---

## Phase 4 Merge Protocol (End of Day 9)

```bash
```bash
git add .
git commit -m "day 9: route quality chart, greedy vs dp panel, bin packing panel,
               dp table viewer, report modal, tabbed layout"
git push origin day/09-frontend-charts-panels
git checkout main
git merge day/09-frontend-charts-panels
git tag v0.4-phase4-complete
```

Update `MEMORY.md` and `PROGRESS.md` before closing.
