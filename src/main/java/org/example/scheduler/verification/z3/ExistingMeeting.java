package org.example.scheduler.verification.z3;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Represents an existing meeting in the system for constraint checking.
 */
public record ExistingMeeting(
    Long meetingId,
    Long roomId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Set<Long> participantIds
) {
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

    /**
     * Checks if this meeting involves a given participant.
     */
    public boolean involvesParticipant(Long participantId) {
        return participantIds != null && participantIds.contains(participantId);
    }
}

