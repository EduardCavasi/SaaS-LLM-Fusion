package org.example.scheduler.verification.runtime;

import java.time.LocalDateTime;

/**
 * Represents a runtime property violation detected by the monitor.
 */
public record PropertyViolation(
    String propertyName,
    String description,
    ViolationSeverity severity,
    Long meetingId,
    LocalDateTime detectedAt,
    String details
) {
    public enum ViolationSeverity {
        WARNING,    // Potential issue that should be investigated
        ERROR,      // Constraint violation that blocks the operation
        CRITICAL    // System invariant violation
    }

    public static PropertyViolation error(String propertyName, String description, 
            Long meetingId, String details) {
        return new PropertyViolation(
            propertyName, description, ViolationSeverity.ERROR, 
            meetingId, LocalDateTime.now(), details
        );
    }

    public static PropertyViolation warning(String propertyName, String description, 
            Long meetingId, String details) {
        return new PropertyViolation(
            propertyName, description, ViolationSeverity.WARNING, 
            meetingId, LocalDateTime.now(), details
        );
    }

    public static PropertyViolation critical(String propertyName, String description, 
            Long meetingId, String details) {
        return new PropertyViolation(
            propertyName, description, ViolationSeverity.CRITICAL, 
            meetingId, LocalDateTime.now(), details
        );
    }
}

