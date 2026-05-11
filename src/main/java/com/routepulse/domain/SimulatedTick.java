package com.routepulse.domain;

/**
 * Value object representing a discrete simulation tick (time step).
 * The simulation advances one tick at a time. Ticks are comparable
 * so they can be used as timestamps in the event queue.
 *
 * @param value the non-negative integer tick count (0 = simulation start)
 */
public record SimulatedTick(int value) implements Comparable<SimulatedTick> {

    /** The initial tick at the start of every simulation run. */
    public static final SimulatedTick ZERO = new SimulatedTick(0);

    public SimulatedTick {
        if (value < 0) {
            throw new IllegalArgumentException(
                    "SimulatedTick value must be non-negative, got: " + value);
        }
    }

    @Override
    public int compareTo(SimulatedTick other) {
        return Integer.compare(this.value, other.value);
    }

    /** Returns the next tick (this.value + 1). */
    public SimulatedTick next() {
        return new SimulatedTick(this.value + 1);
    }
}
