package com.routepulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Central configuration record for all simulation scale parameters and algorithm caps.
 * Every threshold, limit, and constant in the simulation must be sourced from this class —
 * no magic numbers are permitted anywhere in the codebase.
 *
 * <p>Bound from {@code application.yaml} under the {@code simulation.*} prefix.
 * All values here must strictly match the committed scale parameters in SYSTEM_INSTRUCTIONS.md.
 *
 * @param maxTicks               total number of simulation ticks per run (committed: 100)
 * @param dpStopCap              hard cap on stops eligible for DP re-optimization (committed: 10)
 * @param quietPeriodTicks       number of consecutive quiet ticks before QUIET_PERIOD fires
 * @param dpBudgetMs             maximum wall-clock time (ms) allowed for one DP execution
 * @param maxCouriers            maximum number of active couriers per shift (committed: 6)
 * @param bruteForceCapStops     maximum stops at which brute-force verification is run (committed: 5)
 * @param priorityWeight         cost multiplier reduction for HIGH priority orders in greedy insertion
 * @param minImprovementThreshold minimum quality gap (%) required before DP re-optimization fires
 */
@ConfigurationProperties(prefix = "simulation")
public record SimulationConfig(
        int maxTicks,
        int dpStopCap,
        int quietPeriodTicks,
        int dpBudgetMs,
        int maxCouriers,
        int bruteForceCapStops,
        double priorityWeight,
        double minImprovementThreshold) {
}
