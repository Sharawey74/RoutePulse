package com.routepulse.algorithm.api;

/**
 * Marker interface for all algorithm input types.
 * Every algorithm module receives a concrete implementation of this interface
 * as an immutable snapshot — never the live mutable state.
 */
public interface AlgorithmInput {
}
