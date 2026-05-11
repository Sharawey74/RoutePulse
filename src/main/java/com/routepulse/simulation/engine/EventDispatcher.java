package com.routepulse.simulation.engine;

import com.routepulse.algorithm.api.Mutation;
import com.routepulse.simulation.events.Event;
import com.routepulse.simulation.events.EventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Routes simulation events to their corresponding algorithm handlers.
 * Each handler method corresponds to exactly one {@link com.routepulse.simulation.events.EventType}.
 *
 * <p><strong>Current status (Day 2):</strong> All handlers are no-ops — they log the
 * event type and tick, then return a {@link Mutation.NoOpMutation}. Real algorithm
 * calls are wired in Days 3–6 as each algorithm module becomes available.
 *
 * <p><strong>Architecture rule enforced here:</strong>
 * <ul>
 *   <li>No state mutation logic exists in this class — mutations are returned, not applied.</li>
 *   <li>No algorithm logic exists here — the dispatcher only routes and delegates.</li>
 *   <li>The SimulationEngine calls {@link #dispatch(Event)} and passes the result to MutationApplier.</li>
 * </ul>
 */
@Component
public class EventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EventDispatcher.class);

    /**
     * Dispatches an event to its handler based on event type.
     * Uses a switch on {@link com.routepulse.simulation.events.EventType} —
     * the compiler ensures exhaustiveness when new types are added.
     *
     * @param event the event to dispatch
     * @return the Mutation produced by the handler (NoOp in Day 2)
     */
    public Mutation dispatch(Event event) {
        log.debug("EventDispatcher: dispatching {} at tick {}",
                event.type(), event.scheduledTick().value());

        return switch (event.type()) {
            case NEW_ORDER           -> handleNewOrder(event);
            case ROAD_WEIGHT_CHANGE  -> handleRoadWeightChange(event);
            case ROAD_REMOVAL        -> handleRoadRemoval(event);
            case VEHICLE_BREAKDOWN   -> handleVehicleBreakdown(event);
            case PRIORITY_ESCALATION -> handlePriorityEscalation(event);
            case QUIET_PERIOD        -> handleQuietPeriod(event);
        };
    }

    // ── Handlers (no-ops in Day 2) ────────────────────────────────────────────

    /**
     * Handles a NEW_ORDER event.
     *
     * <p><strong>Day 4 algorithm chain:</strong>
     * 1. GreedyInsertionModule.solve(InsertionInput) → RouteInsertionMutation
     * 2. BinPackingModule.solve(PackingInput) → capacity validation
     * 3. MutationApplier applies RouteInsertionMutation + CargoAssignmentMutation
     * 4. ShadowRouteRegistry updated with same insertion (greedy shadow, never DP)
     * 5. EventLogStore.record(metrics)
     *
     * @param event the NEW_ORDER event carrying a NewOrderPayload
     * @return RouteInsertionMutation in Day 4; NoOpMutation in Day 2
     */
    private Mutation handleNewOrder(Event event) {
        EventPayload.NewOrderPayload payload = (EventPayload.NewOrderPayload) event.payload();
        log.info("EventDispatcher: NEW_ORDER tick={} orderId={}",
                event.scheduledTick().value(), payload.order().id());
        return new Mutation.NoOpMutation("NEW_ORDER handler not yet wired — Day 4");
    }

    /**
     * Handles a ROAD_WEIGHT_CHANGE event.
     *
     * <p><strong>Day 3 algorithm chain:</strong>
     * 1. MutationApplier applies EdgeWeightMutation to GraphStore
     * 2. DijkstraModule.solve(from affected courier positions) → DijkstraOutput
     * 3. DistanceMatrix.updateRow() for each affected source node
     * 4. EventLogStore.record(dijkstraMetrics)
     *
     * <p><strong>Rule:</strong> Dijkstra ALWAYS runs before any other algorithm in the same tick.
     *
     * @param event the ROAD_WEIGHT_CHANGE event carrying a RoadWeightChangePayload
     * @return EdgeWeightMutation in Day 3; NoOpMutation in Day 2
     */
    private Mutation handleRoadWeightChange(Event event) {
        EventPayload.RoadWeightChangePayload payload =
                (EventPayload.RoadWeightChangePayload) event.payload();
        log.info("EventDispatcher: ROAD_WEIGHT_CHANGE tick={} from={} to={} weight={}",
                event.scheduledTick().value(), payload.from(), payload.to(), payload.newWeight());
        return new Mutation.NoOpMutation("ROAD_WEIGHT_CHANGE handler not yet wired — Day 3");
    }

    /**
     * Handles a ROAD_REMOVAL event.
     *
     * <p><strong>Day 3 algorithm chain:</strong>
     * 1. MutationApplier applies EdgeRemovalMutation to GraphStore
     * 2. DijkstraModule.solve() for each courier whose route uses this edge
     * 3. DistanceMatrix.updateRow() for affected sources
     * 4. Route re-check: if any courier's route is now disconnected, re-insert via greedy
     *
     * @param event the ROAD_REMOVAL event carrying a RoadRemovalPayload
     * @return EdgeRemovalMutation in Day 3; NoOpMutation in Day 2
     */
    private Mutation handleRoadRemoval(Event event) {
        EventPayload.RoadRemovalPayload payload = (EventPayload.RoadRemovalPayload) event.payload();
        log.info("EventDispatcher: ROAD_REMOVAL tick={} from={} to={}",
                event.scheduledTick().value(), payload.from(), payload.to());
        return new Mutation.NoOpMutation("ROAD_REMOVAL handler not yet wired — Day 3");
    }

    /**
     * Handles a VEHICLE_BREAKDOWN event.
     *
     * <p><strong>Day 5 algorithm chain:</strong>
     * 1. MutationApplier applies CourierDeactivationMutation
     * 2. BinPackingModule(FFD strategy) redistributes broken courier's orders
     * 3. GreedyInsertionModule re-inserts each redistributed order
     * 4. EventLogStore.record(redistributionMetrics)
     *
     * @param event the VEHICLE_BREAKDOWN event carrying a VehicleBreakdownPayload
     * @return CourierDeactivationMutation in Day 5; NoOpMutation in Day 2
     */
    private Mutation handleVehicleBreakdown(Event event) {
        EventPayload.VehicleBreakdownPayload payload =
                (EventPayload.VehicleBreakdownPayload) event.payload();
        log.info("EventDispatcher: VEHICLE_BREAKDOWN tick={} courier={}",
                event.scheduledTick().value(), payload.courierId());
        return new Mutation.NoOpMutation("VEHICLE_BREAKDOWN handler not yet wired — Day 5");
    }

    /**
     * Handles a PRIORITY_ESCALATION event.
     *
     * <p><strong>Day 4 algorithm chain:</strong>
     * 1. OrderRegistry.escalate(orderId) → marks order as HIGH priority
     * 2. Remove current stop from route (RouteInsertionMutation with removal)
     * 3. GreedyInsertionModule.solve() with priority-weighted cost
     * 4. Re-insert at minimum priority-weighted cost position
     *
     * @param event the PRIORITY_ESCALATION event carrying a PriorityEscalationPayload
     * @return RouteInsertionMutation in Day 4; NoOpMutation in Day 2
     */
    private Mutation handlePriorityEscalation(Event event) {
        EventPayload.PriorityEscalationPayload payload =
                (EventPayload.PriorityEscalationPayload) event.payload();
        log.info("EventDispatcher: PRIORITY_ESCALATION tick={} orderId={}",
                event.scheduledTick().value(), payload.orderId());
        return new Mutation.NoOpMutation("PRIORITY_ESCALATION handler not yet wired — Day 4");
    }

    /**
     * Handles a QUIET_PERIOD event.
     *
     * <p><strong>Day 6 algorithm chain:</strong>
     * 1. For each ACTIVE courier with remainingStops ≤ dpStopCap AND improvement gap > threshold:
     *    a. DPReoptimiserModule.solve(DPInput) → DPOutput
     *    b. If DPOutput.isDeferred() → skip this courier
     *    c. MutationApplier applies RouteReorderMutation (NEVER updates shadow route)
     *    d. ShadowComparison: compute quality gap = shadow.distance - dp.distance
     * 2. EventLogStore.record(dpMetrics per courier)
     *
     * <p><strong>Rule:</strong> Shadow route is NEVER updated by DP improvements —
     * it captures greedy-only performance as the baseline.
     *
     * @param event the QUIET_PERIOD event carrying a QuietPeriodPayload
     * @return RouteReorderMutation(s) in Day 6; NoOpMutation in Day 2
     */
    private Mutation handleQuietPeriod(Event event) {
        EventPayload.QuietPeriodPayload payload = (EventPayload.QuietPeriodPayload) event.payload();
        log.info("EventDispatcher: QUIET_PERIOD tick={} quietTicks={}",
                event.scheduledTick().value(), payload.quietTickCount());
        return new Mutation.NoOpMutation("QUIET_PERIOD handler not yet wired — Day 6");
    }
}
