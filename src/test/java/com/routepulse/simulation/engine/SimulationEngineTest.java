package com.routepulse.simulation.engine;

import com.routepulse.config.SimulationConfig;
import com.routepulse.domain.NodeId;
import com.routepulse.domain.SimulatedTick;
import com.routepulse.simulation.events.Event;
import com.routepulse.simulation.events.EventPayload;
import com.routepulse.simulation.events.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationEngineTest {

    private EventQueue queue;
    private SimClock clock;
    private QuietPeriodMonitor monitor;
    private SimulationEngine engine;

    @BeforeEach
    void setUp() {
        queue = new EventQueue();
        clock = new SimClock();
        monitor = new QuietPeriodMonitor();
        EventDispatcher dispatcher = new EventDispatcher();
        MutationApplier applier = new MutationApplier();
        SimulationConfig config = new SimulationConfig(100, 10, 5, 20, 6, 5, 0.5, 0.1);

        engine = new SimulationEngine(queue, clock, dispatcher, applier, monitor, config);
    }

    @Test
    void testEventOrderingAndProcessing() {
        // Actually, NewOrderPayload requires a valid Order. Let's use RoadWeightChange which is simpler.
        Event e3 = Event.builder()
                .scheduledTick(new SimulatedTick(5))
                .createdAt(SimulatedTick.ZERO)
                .type(EventType.ROAD_WEIGHT_CHANGE)
                .payload(new EventPayload.RoadWeightChangePayload(new NodeId(1), new NodeId(2), 10))
                .build();

        Event e4 = Event.builder()
                .scheduledTick(new SimulatedTick(5))
                .createdAt(SimulatedTick.ZERO)
                .type(EventType.ROAD_REMOVAL) // Priority 1 (higher precedence than weight change)
                .payload(new EventPayload.RoadRemovalPayload(new NodeId(1), new NodeId(2)))
                .build();

        queue.enqueue(e3);
        queue.enqueue(e4);

        assertEquals(2, queue.size());

        // Process up to tick 5
        engine.run(6);
        assertEquals(6, clock.currentTick().value());
        assertTrue(queue.isEmpty(), "Events at tick 5 should be processed");
    }

    @Test
    void testQuietPeriodMonitor() {
        // Enqueue a disruption at tick 2
        engine.enqueue(Event.builder()
                .scheduledTick(new SimulatedTick(2))
                .createdAt(SimulatedTick.ZERO)
                .type(EventType.ROAD_REMOVAL)
                .payload(new EventPayload.RoadRemovalPayload(new NodeId(1), new NodeId(2)))
                .build());

        // Tick 0, 1: no disruptions. Monitor: 0 -> 1 -> 2
        // Tick 2: disruption. Monitor: resets to 0, then tick() -> 1
        // Tick 3: no disruption -> 2
        // Tick 4: no disruption -> 3
        // Tick 5: no disruption -> 4
        // Tick 6: no disruption -> 5 -> fires QUIET_PERIOD for tick 7

        engine.run(7); // Run 0 to 6

        assertEquals(1, queue.size());
        assertEquals(EventType.QUIET_PERIOD, queue.peek().get().type());
        assertEquals(7, queue.peek().get().scheduledTick().value());
    }
}
