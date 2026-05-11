package com.routepulse.simulation.events;

import com.routepulse.domain.SimulatedTick;

/**
 * Immutable representation of a discrete simulation event.
 * Events are the fundamental unit of work in the simulation — everything
 * that happens during a run is triggered by processing an event.
 *
 * <p>Events are ordered first by {@link #scheduledTick} (ascending),
 * then by {@link #priority} level (ascending = higher precedence) when
 * ticks are equal. This ordering is enforced by the {@link EventQueue}.
 *
 * <p><strong>Construction:</strong> Use {@link Event#builder()} — direct
 * constructor instantiation is intentionally not provided.
 */
public final class Event {

    private final SimulatedTick scheduledTick;
    private final SimulatedTick createdAt;
    private final EventType type;
    private final EventPriority priority;
    private final EventPayload payload;

    private Event(Builder builder) {
        this.scheduledTick = builder.scheduledTick;
        this.createdAt     = builder.createdAt;
        this.type          = builder.type;
        this.priority      = builder.priority;
        this.payload       = builder.payload;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    /** Returns the tick at which this event is scheduled to be processed. */
    public SimulatedTick scheduledTick() { return scheduledTick; }

    /** Returns the tick at which this event was created and enqueued. */
    public SimulatedTick createdAt() { return createdAt; }

    /** Returns the event type, which determines the handler and algorithm chain. */
    public EventType type() { return type; }

    /** Returns the priority level, which determines processing order within a tick. */
    public EventPriority priority() { return priority; }

    /** Returns the typed payload containing all data the handler needs. */
    public EventPayload payload() { return payload; }

    @Override
    public String toString() {
        return "Event{tick=" + scheduledTick.value()
                + ", type=" + type
                + ", priority=" + priority.level()
                + ", createdAt=" + createdAt.value() + "}";
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    /** Returns a new fluent builder for constructing an Event instance. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link Event}.
     * All fields except {@code priority} are required.
     * If {@code priority} is not set, it is derived automatically from the event type.
     */
    public static final class Builder {

        private SimulatedTick scheduledTick;
        private SimulatedTick createdAt;
        private EventType type;
        private EventPriority priority;
        private EventPayload payload;

        private Builder() {}

        /** Sets the tick at which this event will be processed. */
        public Builder scheduledTick(SimulatedTick scheduledTick) {
            this.scheduledTick = scheduledTick;
            return this;
        }

        /** Sets the tick at which this event was created. */
        public Builder createdAt(SimulatedTick createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the event type. Also auto-derives priority from the type
         * if {@link #priority(EventPriority)} has not been called explicitly.
         */
        public Builder type(EventType type) {
            this.type = type;
            if (this.priority == null) {
                this.priority = EventPriority.forType(type);
            }
            return this;
        }

        /**
         * Overrides the auto-derived priority.
         * Must be called after {@link #type(EventType)} if an override is needed.
         */
        public Builder priority(EventPriority priority) {
            this.priority = priority;
            return this;
        }

        /** Sets the typed payload for this event. */
        public Builder payload(EventPayload payload) {
            this.payload = payload;
            return this;
        }

        /**
         * Builds and returns the immutable Event.
         *
         * @throws IllegalStateException if any required field is null
         */
        public Event build() {
            validateRequired("scheduledTick", scheduledTick);
            validateRequired("createdAt", createdAt);
            validateRequired("type", type);
            validateRequired("priority", priority);
            validateRequired("payload", payload);
            return new Event(this);
        }

        private void validateRequired(String fieldName, Object value) {
            if (value == null) {
                throw new IllegalStateException(
                        "Event.Builder: required field '" + fieldName + "' must not be null");
            }
        }
    }
}
