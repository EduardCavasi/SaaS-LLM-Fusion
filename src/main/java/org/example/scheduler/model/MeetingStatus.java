package org.example.scheduler.model;

//meeting status for runtime verification
public enum MeetingStatus {
    /**
     * Meeting has been created but not yet confirmed
     */
    PENDING,
    
    /**
     * Meeting has been confirmed and is scheduled
     */
    CONFIRMED,
    
    /**
     * Meeting has been rejected due to constraint violations
     */
    REJECTED,
    
    /**
     * Meeting has been cancelled
     */
    CANCELLED,
    
    /**
     * Meeting has been completed
     */
    COMPLETED
}

