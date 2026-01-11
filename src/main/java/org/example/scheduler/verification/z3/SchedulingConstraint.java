package org.example.scheduler.verification.z3;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Represents a scheduling constraint for Z3 solver.
 * Encapsulates meeting scheduling request parameters.
 */
public record SchedulingConstraint(
    Long meetingId,
    Long roomId,
    int roomCapacity,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Set<Long> participantIds
) {
    /**
     * Validates that the constraint has valid time bounds.
     */
    public boolean hasValidTimeRange() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }

    /**
     * Checks if the number of participants fits the room capacity.
     */
    public boolean fitsCapacity() {
        return participantIds != null && participantIds.size() <= roomCapacity;
    }

    /**
     * Converts start time to epoch seconds for Z3 arithmetic.
     */
    public long getStartEpochSecond() {
        return startTime.toEpochSecond(java.time.ZoneOffset.UTC);
    }

    /**
     * Converts end time to epoch seconds for Z3 arithmetic.
     */
    public long getEndEpochSecond() {
        return endTime.toEpochSecond(java.time.ZoneOffset.UTC);
    }
}

