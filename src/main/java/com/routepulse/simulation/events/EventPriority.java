package com.routepulse.simulation.events;

/**
 * Enumeration of event processing priority levels within the same simulation tick.
 * Lower ordinal value = higher processing precedence.
 *
 * <p>When multiple events share the same scheduled tick, they are processed in this order:
 * <ol>
 *   <li>{@link #ROAD_REMOVAL} (1) — graph structure changes must be resolved first</li>
 *   <li>{@link #ROAD_WEIGHT_CHANGE} (2) — weight updates after structural changes</li>
 *   <li>{@link #VEHICLE_BREAKDOWN} (3) — redistribution uses the updated graph</li>
 *   <li>{@link #NEW_ORDER} (4) — insertions use the updated distances and fleets</li>
 *   <li>{@link #PRIORITY_ESCALATION} (5) — re-insertion after new orders are placed</li>
 *   <li>{@link #QUIET_PERIOD} (6) — DP optimization fires last in the tick</li>
 * </ol>
 *
 * <p>This ordering is architecturally non-negotiable: Dijkstra must always complete
 * before any route insertion or re-optimization in the same tick.
 */
public enum EventPriority {

    /** Highest precedence — road removal changes graph topology. */
    ROAD_REMOVAL(1),

    /** Second — road weight changes update traversal costs. */
    ROAD_WEIGHT_CHANGE(2),

    /** Third — vehicle breakdown triggers fleet redistribution. */
    VEHICLE_BREAKDOWN(3),

    /** Fourth — new order insertion uses current fleet and distances. */
    NEW_ORDER(4),

    /** Fifth — priority escalation re-inserts after new orders settled. */
    PRIORITY_ESCALATION(5),

    /** Lowest precedence — DP fires as the final operation of the tick. */
    QUIET_PERIOD(6);

    /** Numeric priority level — lower value means higher processing precedence. */
    private final int level;

    EventPriority(int level) {
        this.level = level;
    }

    /** Returns the numeric priority level for comparator use. */
    public int level() {
        return level;
    }

    /**
     * Returns the EventPriority that corresponds to the given EventType.
     * This mapping encodes the tick-processing ordering rule from SYSTEM_INSTRUCTIONS.md.
     *
     * @param type the event type to look up
     * @return the corresponding priority level
     */
    public static EventPriority forType(EventType type) {
        return switch (type) {
            case ROAD_REMOVAL        -> ROAD_REMOVAL;
            case ROAD_WEIGHT_CHANGE  -> ROAD_WEIGHT_CHANGE;
            case VEHICLE_BREAKDOWN   -> VEHICLE_BREAKDOWN;
            case NEW_ORDER           -> NEW_ORDER;
            case PRIORITY_ESCALATION -> PRIORITY_ESCALATION;
            case QUIET_PERIOD        -> QUIET_PERIOD;
        };
    }
}
