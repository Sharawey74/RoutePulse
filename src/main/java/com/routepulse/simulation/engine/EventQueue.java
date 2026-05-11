package com.routepulse.simulation.engine;

import com.routepulse.simulation.events.Event;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Priority-ordered event queue for the discrete-event simulation.
 * Maintains events sorted by: scheduledTick ascending, then priority level ascending
 * (lower priority level = higher precedence within the same tick).
 *
 * <p><strong>Implementation note:</strong> Uses a sorted {@link ArrayList} — correct
 * and simple at ≤200 events per simulation run. Replace with a {@link java.util.PriorityQueue}
 * if queue depth grows beyond 500 events.
 *
 * <p><strong>Mutation contract:</strong> Only {@code SimulationEngine} may call
 * {@link #enqueue(Event)} and {@link #dequeue()}. Other components use read-only access.
 */
@Component
public class EventQueue {

    /** Comparator: ascending tick, then ascending priority level (lower = higher precedence). */
    private static final Comparator<Event> EVENT_ORDER = Comparator
            .comparingInt((Event e) -> e.scheduledTick().value())
            .thenComparingInt(e -> e.priority().level());

    /** Internal sorted list. Head (index 0) is always the next event to process. */
    private final List<Event> events = new ArrayList<>();

    /**
     * Inserts an event into its correct sorted position.
     * Complexity: O(n) binary search insertion into sorted list.
     * Acceptable at ≤200 events per committed scale parameters.
     *
     * @param event the event to enqueue, must not be null
     */
    public void enqueue(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Cannot enqueue a null event");
        }
        int insertionIndex = findInsertionIndex(event);
        events.add(insertionIndex, event);
    }

    /**
     * Removes and returns the highest-priority event (head of the sorted list).
     *
     * @return the next event to process
     * @throws NoSuchElementException if the queue is empty
     */
    public Event dequeue() {
        if (events.isEmpty()) {
            throw new NoSuchElementException("EventQueue is empty — cannot dequeue");
        }
        return events.remove(0);
    }

    /**
     * Returns the highest-priority event without removing it.
     *
     * @return Optional containing the next event, or empty if the queue is empty
     */
    public Optional<Event> peek() {
        return events.isEmpty() ? Optional.empty() : Optional.of(events.get(0));
    }

    /** Returns true if there are no events in the queue. */
    public boolean isEmpty() {
        return events.isEmpty();
    }

    /** Returns the number of events currently in the queue. */
    public int size() {
        return events.size();
    }

    /** Removes all events from the queue. Used for simulation reset. */
    public void clear() {
        events.clear();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Finds the correct insertion index to maintain sorted order.
     * Uses linear scan from the tail to find the first position where the
     * new event sorts before the existing one.
     *
     * @param event the event to insert
     * @return the index at which to insert the event
     */
    private int findInsertionIndex(Event event) {
        int low = 0;
        int high = events.size();
        // Binary search for insertion point
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (EVENT_ORDER.compare(events.get(mid), event) <= 0) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }
}
