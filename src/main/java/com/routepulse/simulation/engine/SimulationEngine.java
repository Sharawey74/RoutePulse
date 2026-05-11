package com.routepulse.simulation.engine;

import com.routepulse.algorithm.api.Mutation;
import com.routepulse.config.SimulationConfig;
import com.routepulse.domain.SimulatedTick;
import com.routepulse.simulation.events.Event;
import com.routepulse.simulation.events.EventPayload;
import com.routepulse.simulation.events.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Central orchestrator of the discrete-event simulation.
 * The SimulationEngine drives the tick-by-tick execution loop,
 * coordinating the event queue, dispatcher, mutation applier, clock,
 * and quiet period monitor.
 *
 * <p><strong>Architecture rules enforced here:</strong>
 * <ul>
 *   <li>No algorithm logic exists in this class — algorithms are called via EventDispatcher.</li>
 *   <li>No state mutation logic exists here — all mutations go through MutationApplier.</li>
 *   <li>This class depends on the {@code AlgorithmModule} interface, never on concrete classes.</li>
 * </ul>
 *
 * <p><strong>Step sequence per tick (from SYSTEM_INSTRUCTIONS.md):</strong>
 * <ol>
 *   <li>If queue empty → return (no-op tick)</li>
 *   <li>While the next event's tick equals currentTick: dequeue + dispatch + apply</li>
 *   <li>If event is a disruption → {@code monitor.recordDisruption()}</li>
 *   <li>{@code monitor.tick()}</li>
 *   <li>If quiet period reached → enqueue QUIET_PERIOD event + monitor.reset()</li>
 *   <li>{@code clock.advance()}</li>
 * </ol>
 */
@Component
public class SimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

    private final EventQueue eventQueue;
    private final SimClock clock;
    private final EventDispatcher dispatcher;
    private final MutationApplier mutationApplier;
    private final QuietPeriodMonitor quietPeriodMonitor;
    private final SimulationConfig config;

    /** No-op state placeholder until Day 3 wires in the real state aggregator. */
    private final SimulationState noOpState = new SimulationState() {};

    public SimulationEngine(EventQueue eventQueue,
                            SimClock clock,
                            EventDispatcher dispatcher,
                            MutationApplier mutationApplier,
                            QuietPeriodMonitor quietPeriodMonitor,
                            SimulationConfig config) {
        this.eventQueue        = eventQueue;
        this.clock             = clock;
        this.dispatcher        = dispatcher;
        this.mutationApplier   = mutationApplier;
        this.quietPeriodMonitor = quietPeriodMonitor;
        this.config            = config;
    }

    /**
     * Processes all events scheduled for the current tick, then advances the clock.
     * Multiple events can share the same tick — they are processed in priority order
     * (guaranteed by EventQueue's sorted ordering).
     *
     * <p>If the event queue is empty, this is a no-op tick (clock still advances).
     */
    public void step() {
        SimulatedTick currentTick = clock.currentTick();
        log.debug("SimulationEngine: step() tick={}", currentTick.value());

        boolean disruptionOccurred = processEventsForCurrentTick(currentTick);

        if (disruptionOccurred) {
            quietPeriodMonitor.recordDisruption();
        }

        quietPeriodMonitor.tick();

        if (quietPeriodMonitor.isQuietPeriodReached(config.quietPeriodTicks())) {
            enqueueQuietPeriodEvent(currentTick);
            quietPeriodMonitor.reset();
        }

        clock.advance();
    }

    /**
     * Runs the simulation for exactly {@code ticks} steps.
     * Stops early if the event queue is empty before the tick limit is reached.
     *
     * @param ticks the number of steps to execute (must not exceed SimulationConfig.maxTicks)
     * @throws IllegalArgumentException if ticks exceeds the configured maximum
     */
    public void run(int ticks) {
        if (ticks > config.maxTicks()) {
            throw new IllegalArgumentException(
                    "Requested ticks " + ticks + " exceeds maxTicks " + config.maxTicks());
        }
        log.info("SimulationEngine: run() starting — {} ticks requested", ticks);
        for (int i = 0; i < ticks; i++) {
            step();
        }
        log.info("SimulationEngine: run() complete at tick {}", clock.currentTick().value());
    }

    /**
     * Resets the simulation to its initial state.
     * Clears the event queue, resets the clock and quiet period monitor.
     * The state layer objects (GraphStore, etc.) are reset separately via their own reset methods.
     */
    public void reset() {
        log.info("SimulationEngine: reset()");
        eventQueue.clear();
        clock.reset();
        quietPeriodMonitor.reset();
    }

    /**
     * Returns the current simulation clock tick.
     *
     * @return current tick value
     */
    public SimulatedTick currentTick() {
        return clock.currentTick();
    }

    /**
     * Enqueues an event into the event queue.
     * Exposed for use by ScenarioLoader and test scaffolding.
     *
     * @param event the event to schedule
     */
    public void enqueue(Event event) {
        eventQueue.enqueue(event);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Processes all events whose scheduledTick equals the current tick.
     * Events are dequeued and processed one at a time until the queue is empty
     * or the next event belongs to a future tick.
     *
     * @param currentTick the tick being processed
     * @return true if any disruption event was processed during this tick
     */
    private boolean processEventsForCurrentTick(SimulatedTick currentTick) {
        boolean disruptionOccurred = false;

        while (true) {
            Optional<Event> next = eventQueue.peek();
            if (next.isEmpty()) break;

            Event nextEvent = next.get();
            if (nextEvent.scheduledTick().value() > currentTick.value()) break;

            Event event = eventQueue.dequeue();
            Mutation mutation = dispatcher.dispatch(event);
            mutationApplier.apply(mutation, noOpState);

            if (isDisruptionEvent(event.type())) {
                disruptionOccurred = true;
            }
        }

        return disruptionOccurred;
    }

    /**
     * Returns true if the given event type counts as a disruption for the quiet period monitor.
     * Disruption events: ROAD_REMOVAL, ROAD_WEIGHT_CHANGE, VEHICLE_BREAKDOWN.
     *
     * @param type the event type to check
     * @return true if this event type resets the quiet tick counter
     */
    private boolean isDisruptionEvent(EventType type) {
        return switch (type) {
            case ROAD_REMOVAL, ROAD_WEIGHT_CHANGE, VEHICLE_BREAKDOWN -> true;
            case NEW_ORDER, PRIORITY_ESCALATION, QUIET_PERIOD        -> false;
        };
    }

    /**
     * Enqueues a QUIET_PERIOD event scheduled for the next tick.
     *
     * @param currentTick the tick at which the quiet period threshold was reached
     */
    private void enqueueQuietPeriodEvent(SimulatedTick currentTick) {
        SimulatedTick nextTick = currentTick.next();
        Event quietPeriodEvent = Event.builder()
                .scheduledTick(nextTick)
                .createdAt(currentTick)
                .type(EventType.QUIET_PERIOD)
                .payload(new EventPayload.QuietPeriodPayload(config.quietPeriodTicks()))
                .build();
        eventQueue.enqueue(quietPeriodEvent);
        log.info("SimulationEngine: QUIET_PERIOD enqueued for tick {}", nextTick.value());
    }
}
