package com.routepulse.simulation.engine;

import com.routepulse.domain.SimulatedTick;
import org.springframework.stereotype.Component;

/**
 * Discrete simulation clock — maintains the current tick count.
 * The clock is the single source of truth for simulation time.
 *
 * <p><strong>Mutation contract:</strong> Only {@code SimulationEngine} may call
 * {@link #advance()} and {@link #reset()}. All other components read via
 * {@link #currentTick()} only.
 */
@Component
public class SimClock {

    private SimulatedTick currentTick = SimulatedTick.ZERO;

    /**
     * Returns the current simulation tick.
     *
     * @return the current immutable tick value, never null
     */
    public SimulatedTick currentTick() {
        return currentTick;
    }

    /**
     * Advances the clock by exactly one tick.
     * Called by SimulationEngine at the end of each step.
     */
    public void advance() {
        currentTick = currentTick.next();
    }

    /**
     * Resets the clock to tick zero.
     * Called by SimulationEngine.reset() at the start of a new simulation run.
     */
    public void reset() {
        currentTick = SimulatedTick.ZERO;
    }
}
