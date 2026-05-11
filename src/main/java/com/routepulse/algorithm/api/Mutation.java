package com.routepulse.algorithm.api;

import com.routepulse.domain.CourierId;
import com.routepulse.domain.NodeId;
import com.routepulse.domain.Stop;

import java.util.List;

/**
 * Sealed interface representing a typed, immutable state mutation command.
 * All state changes in the simulation are expressed as Mutation objects
 * returned by algorithm modules. The MutationApplier is the sole consumer
 * responsible for applying them to the live state.
 *
 * <p>Every permitted subtype represents a single, atomic, named state change.
 * New mutation types must be added as new permits — no existing type is modified (OCP).
 *
 * <p><strong>Type inventory:</strong>
 * <ul>
 *   <li>{@link NoOpMutation}            — no state change required</li>
 *   <li>{@link EdgeWeightMutation}      — update graph edge weight + trigger distance matrix row update</li>
 *   <li>{@link EdgeRemovalMutation}     — remove a directed edge from the graph</li>
 *   <li>{@link RouteInsertionMutation}  — insert a stop into a courier's route at a given position</li>
 *   <li>{@link RouteReorderMutation}    — replace a courier's stop ordering with a DP-optimised sequence</li>
 *   <li>{@link CourierDeactivationMutation} — mark a courier as BROKEN_DOWN</li>
 *   <li>{@link CargoAssignmentMutation} — assign a cargo item to a courier's cargo manifest</li>
 * </ul>
 */
public sealed interface Mutation
        permits Mutation.NoOpMutation,
                Mutation.EdgeWeightMutation,
                Mutation.EdgeRemovalMutation,
                Mutation.RouteInsertionMutation,
                Mutation.RouteReorderMutation,
                Mutation.CourierDeactivationMutation,
                Mutation.CargoAssignmentMutation {

    /**
     * A sentinel no-operation mutation.
     * Used when an algorithm determines no state change is required.
     *
     * @param reason human-readable explanation of why no change was made
     */
    record NoOpMutation(String reason) implements Mutation {
    }

    /**
     * Updates the weight of a directed edge in the graph.
     * After application, MutationApplier re-runs Dijkstra from the source node
     * and updates the affected row in the DistanceMatrix.
     *
     * @param from      source node of the edge
     * @param to        destination node of the edge
     * @param newWeight the new traversal cost (1–20)
     */
    record EdgeWeightMutation(NodeId from, NodeId to, int newWeight) implements Mutation {
    }

    /**
     * Removes a directed edge from the graph entirely.
     * After application, MutationApplier re-runs Dijkstra for all couriers
     * whose current routes pass through this edge.
     *
     * @param from source node of the edge to remove
     * @param to   destination node of the edge to remove
     */
    record EdgeRemovalMutation(NodeId from, NodeId to) implements Mutation {
    }

    /**
     * Inserts a new stop into a courier's route at the specified position.
     * This is the output of the GreedyInsertionModule — the position is the
     * minimum-cost insertion index computed by the O(k×m) algorithm.
     *
     * @param courierId      the courier receiving the new stop
     * @param stop           the stop to insert
     * @param insertionIndex the 0-based index at which to insert in the route's stop list
     */
    record RouteInsertionMutation(CourierId courierId, Stop stop, int insertionIndex)
            implements Mutation {
    }

    /**
     * Replaces a courier's stop sequence with the DP-optimised ordering.
     * Only stops with status PENDING are reordered — IN_TRANSIT and DELIVERED stops
     * are never moved (they are stripped from the DP input).
     *
     * @param courierId    the courier whose route is being reordered
     * @param reorderedStops the full new stop list in DP-optimal order
     */
    record RouteReorderMutation(CourierId courierId, List<Stop> reorderedStops)
            implements Mutation {
        public RouteReorderMutation {
            // Defensive copy to ensure the stored list is immutable
            reorderedStops = List.copyOf(reorderedStops);
        }
    }

    /**
     * Marks a courier as BROKEN_DOWN, removing them from the active fleet.
     * After application, MutationApplier retrieves their pending orders for redistribution.
     *
     * @param courierId the courier to deactivate
     */
    record CourierDeactivationMutation(CourierId courierId) implements Mutation {
    }

    /**
     * Assigns a cargo item to a specific courier's cargo manifest.
     * Applied after a successful RouteInsertionMutation to keep cargo and route in sync.
     *
     * @param courierId the courier receiving the cargo
     * @param orderId   the order identifier of the cargo being assigned
     * @param weight    cargo weight
     * @param volume    cargo volume
     */
    record CargoAssignmentMutation(CourierId courierId, String orderId,
                                   double weight, double volume) implements Mutation {
    }
}
