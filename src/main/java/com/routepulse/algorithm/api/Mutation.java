package com.routepulse.algorithm.api;

/**
 * Sealed interface representing a typed, immutable state mutation command.
 * All state changes in the simulation are expressed as Mutation objects
 * returned by algorithm modules. The MutationApplier is the sole consumer
 * responsible for applying them to the live state.
 *
 * <p>Every permitted subtype represents a single, atomic, named state change.
 * New mutation types must be added as new permits — no existing type is modified.
 */
public sealed interface Mutation
        permits Mutation.NoOpMutation {

    /**
     * A sentinel no-operation mutation.
     * Used when an algorithm determines no state change is required.
     *
     * @param reason human-readable explanation of why no change was made
     */
    record NoOpMutation(String reason) implements Mutation {
    }
}
