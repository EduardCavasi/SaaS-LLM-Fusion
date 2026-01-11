package org.example.scheduler.verification.z3;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Represents a scheduling constraint for Z3 solver.
 */
public record SchedulingConstraint(
    Long meetingId,
    Long roomId,
    int roomCapacity,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Set<Long> participantIds
) {

    public boolean hasValidTimeRange() {
        return startTime != null && endTime != null && startTime.isBefore(endTime);
    }

    public boolean fitsCapacity() {
        return participantIds != null && participantIds.size() <= roomCapacity;
    }

    public long getStartEpochSecond() {
        return startTime.toEpochSecond(java.time.ZoneOffset.UTC);
    }

    public long getEndEpochSecond() {
        return endTime.toEpochSecond(java.time.ZoneOffset.UTC);
    }
}

