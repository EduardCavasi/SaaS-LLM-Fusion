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

    public long getStartEpochSecond() {
        return startTime.toEpochSecond(java.time.ZoneOffset.UTC);
    }

    public long getEndEpochSecond() {
        return endTime.toEpochSecond(java.time.ZoneOffset.UTC);
    }

    public boolean involvesParticipant(Long participantId) {
        return participantIds != null && participantIds.contains(participantId);
    }
}

