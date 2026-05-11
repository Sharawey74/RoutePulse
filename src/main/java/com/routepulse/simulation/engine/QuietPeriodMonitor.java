package com.routepulse.simulation.engine;

import org.springframework.stereotype.Component;

/**
 * Monitors consecutive quiet ticks to determine when a QUIET_PERIOD event should fire.
 * A "quiet tick" is any tick in which no disruption event
 * (ROAD_REMOVAL, ROAD_WEIGHT_CHANGE, VEHICLE_BREAKDOWN) was processed.
 *
 * <p>When {@link #isQuietPeriodReached(int)} returns true, the SimulationEngine
 * enqueues a QUIET_PERIOD event and calls {@link #reset()} on this monitor,
 * restarting the quiet tick counter.
 *
 * <p><strong>Mutation contract:</strong> Only {@code SimulationEngine} may call
 * {@link #recordDisruption()}, {@link #tick()}, and {@link #reset()}.
 */
@Component
public class QuietPeriodMonitor {

    /** Count of consecutive ticks that have passed without a disruption event. */
    private int ticksSinceLastDisruption = 0;

    /**
     * Records that a disruption event occurred in the current tick.
     * Resets the quiet tick counter to zero.
     * Call this for every ROAD_REMOVAL, ROAD_WEIGHT_CHANGE, or VEHICLE_BREAKDOWN event.
     */
    public void recordDisruption() {
        ticksSinceLastDisruption = 0;
    }

    /**
     * Advances the quiet tick counter by one.
     * Called once per simulation step by SimulationEngine, after disruption check.
     */
    public void tick() {
        ticksSinceLastDisruption++;
    }

    /**
     * Returns true if the quiet tick count has reached or exceeded the given threshold.
     * When true, the SimulationEngine should enqueue a QUIET_PERIOD event.
     *
     * @param threshold the number of consecutive quiet ticks required to trigger DP
     * @return true if threshold is reached or exceeded
     */
    public boolean isQuietPeriodReached(int threshold) {
        return ticksSinceLastDisruption >= threshold;
    }

    /**
     * Returns the current count of consecutive quiet ticks (for diagnostics/logging).
     *
     * @return number of quiet ticks since the last disruption
     */
    public int ticksSinceLastDisruption() {
        return ticksSinceLastDisruption;
    }

    /**
     * Resets the quiet tick counter to zero.
     * Called by SimulationEngine after a QUIET_PERIOD event is enqueued,
     * and also during simulation reset.
     */
    public void reset() {
        ticksSinceLastDisruption = 0;
    }
}
