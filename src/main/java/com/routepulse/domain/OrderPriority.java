package com.routepulse.domain;

/**
 * Enumeration of delivery order priority levels.
 * Priority affects the greedy insertion cost weighting:
 * high-priority orders pay a lower effective cost, ensuring they
 * are inserted at better positions in the route.
 */
public enum OrderPriority {
    /** Standard priority — inserted at minimum-cost position. */
    STANDARD,
    /** High priority — cost weighted by SimulationConfig.priorityWeight during insertion. */
    HIGH
}
