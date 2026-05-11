package com.routepulse.simulation.engine;

import com.routepulse.algorithm.api.Mutation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Applies typed {@link Mutation} commands to the live simulation state.
 * This is the single point of state mutation in the entire simulation —
 * no other class is permitted to modify state directly.
 *
 * <p><strong>Current status (Day 2):</strong> All mutation handlers are no-ops
 * that log the mutation type. Real implementations are wired in Day 3
 * when the full state layer objects (GraphStore, RouteRegistry, etc.) are ready.
 *
 * <p><strong>Design pattern:</strong> Command — each Mutation is a typed command
 * object that carries all data needed for its own application. The switch
 * uses Java 21 pattern matching on the sealed interface for exhaustive dispatch.
 */
@Component
public class MutationApplier {

    private static final Logger log = LoggerFactory.getLogger(MutationApplier.class);

    /**
     * Dispatches the given mutation to its handler and applies the state change.
     * Pattern-matches exhaustively on the sealed {@link Mutation} interface —
     * the compiler guarantees no type is unhandled.
     *
     * @param mutation the typed mutation command returned by an algorithm module
     * @param state    the live simulation state to mutate (no-op state in Day 2)
     */
    public void apply(Mutation mutation, SimulationState state) {
        switch (mutation) {
            case Mutation.NoOpMutation m ->
                    log.debug("MutationApplier: no-op — reason={}", m.reason());

            case Mutation.EdgeWeightMutation m ->
                    // Day 3: graphStore.updateEdgeWeight(m.from(), m.to(), m.newWeight())
                    //        dijkstra re-run from m.from()
                    //        distanceMatrix.updateRow(m.from(), dijkstraOutput.distanceArray())
                    log.debug("MutationApplier: EdgeWeightMutation from={} to={} weight={}",
                            m.from(), m.to(), m.newWeight());

            case Mutation.EdgeRemovalMutation m ->
                    // Day 3: graphStore.removeEdge(m.from(), m.to())
                    //        dijkstra re-run for all couriers whose routes pass through this edge
                    log.debug("MutationApplier: EdgeRemovalMutation from={} to={}",
                            m.from(), m.to());

            case Mutation.RouteInsertionMutation m ->
                    // Day 4: routeRegistry.insertStop(m.courierId(), m.stop(), m.insertionIndex())
                    //        shadowRouteRegistry.insertStop(m.courierId(), m.stop(), m.insertionIndex())
                    log.debug("MutationApplier: RouteInsertionMutation courier={} at index={}",
                            m.courierId(), m.insertionIndex());

            case Mutation.RouteReorderMutation m ->
                    // Day 6: routeRegistry.reorder(m.courierId(), m.reorderedStops())
                    //        NOTE: shadow route is NEVER updated by DP reorder (by design)
                    log.debug("MutationApplier: RouteReorderMutation courier={} stops={}",
                            m.courierId(), m.reorderedStops().size());

            case Mutation.CourierDeactivationMutation m ->
                    // Day 5: courierRegistry.deactivate(m.courierId())
                    log.debug("MutationApplier: CourierDeactivationMutation courier={}",
                            m.courierId());

            case Mutation.CargoAssignmentMutation m ->
                    // Day 4: cargoRegistry.assign(m.courierId(), new CargoItem(m.orderId(), m.weight(), m.volume()))
                    log.debug("MutationApplier: CargoAssignmentMutation courier={} order={}",
                            m.courierId(), m.orderId());
        }
    }
}
