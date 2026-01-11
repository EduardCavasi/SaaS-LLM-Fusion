package org.example.scheduler.verification.runtime;

import org.example.scheduler.model.MeetingStatus;

import java.time.LocalDateTime;

/**
 * Represents a meeting lifecycle event for runtime verification.
 */
public record MeetingEvent(
    EventType eventType,
    Long meetingId,
    Long roomId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    int participantCount,
    MeetingStatus previousStatus,
    MeetingStatus newStatus,
    LocalDateTime eventTimestamp
) {
    public enum EventType {
        CREATE,
        UPDATE,
        DELETE,
        CONFIRM,
        REJECT,
        CANCEL,
        COMPLETE
    }

    public static MeetingEvent create(Long meetingId, Long roomId, 
            LocalDateTime startTime, LocalDateTime endTime, int participantCount) {
        return new MeetingEvent(
            EventType.CREATE, meetingId, roomId, startTime, endTime, 
            participantCount, null, MeetingStatus.PENDING, LocalDateTime.now()
        );
    }

    public static MeetingEvent confirm(Long meetingId) {
        return new MeetingEvent(
            EventType.CONFIRM, meetingId, null, null, null, 
            0, MeetingStatus.PENDING, MeetingStatus.CONFIRMED, LocalDateTime.now()
        );
    }

    public static MeetingEvent reject(Long meetingId) {
        return new MeetingEvent(
            EventType.REJECT, meetingId, null, null, null, 
            0, MeetingStatus.PENDING, MeetingStatus.REJECTED, LocalDateTime.now()
        );
    }

    public static MeetingEvent delete(Long meetingId, MeetingStatus previousStatus) {
        return new MeetingEvent(
            EventType.DELETE, meetingId, null, null, null, 
            0, previousStatus, null, LocalDateTime.now()
        );
    }

    public static MeetingEvent cancel(Long meetingId, MeetingStatus previousStatus) {
        return new MeetingEvent(
            EventType.CANCEL, meetingId, null, null, null, 
            0, previousStatus, MeetingStatus.CANCELLED, LocalDateTime.now()
        );
    }
}

