package com.routepulse.algorithm.api;

/**
 * Strategy contract for all algorithm modules in the simulation.
 * Each algorithm module is a pure function: given an immutable input snapshot,
 * it returns a structured output. No side effects are permitted.
 *
 * <p>The SimulationEngine interacts exclusively with this interface,
 * never with concrete algorithm implementations (Dependency Inversion Principle).
 *
 * @param <I> the specific AlgorithmInput type this module consumes
 * @param <O> the specific AlgorithmOutput type this module produces
 */
public interface AlgorithmModule<I extends AlgorithmInput, O extends AlgorithmOutput> {

    /**
     * Executes the algorithm on the given immutable input and returns the result.
     * Must be a pure function: same input always produces same output,
     * no shared mutable state is read or written.
     *
     * @param input immutable snapshot of all data the algorithm requires
     * @return the computed output, never null
     */
    O solve(I input);

    /**
     * Returns the human-readable name of this algorithm.
     * Used for logging, metrics, and report generation.
     *
     * @return non-null, non-empty algorithm name
     */
    String algorithmName();

    /**
     * Returns the reported time and space complexity of this algorithm.
     * Used in the final simulation report for educational comparison.
     *
     * @return complexity descriptor, never null
     */
    AlgorithmComplexity reportedComplexity();
}
