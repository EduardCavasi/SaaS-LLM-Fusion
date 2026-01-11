package org.example.scheduler.verification.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.example.scheduler.dto.MeetingDTO;
import org.example.scheduler.dto.SchedulingResultDTO;
import org.example.scheduler.model.Meeting;
import org.example.scheduler.model.MeetingStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Aspect for automatic runtime verification of meeting operations.
 * 
 * Intercepts service method calls to automatically invoke the MeetingMonitor.
 * This provides transparent runtime verification without modifying service code.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingMonitorAspect {

    private final MeetingMonitor meetingMonitor;

    /**
     * Pointcut for meeting creation methods.
     */
    @Pointcut("execution(* org.example.scheduler.service.MeetingService.createMeeting(..))")
    public void meetingCreation() {}

    /**
     * Pointcut for meeting update methods.
     */
    @Pointcut("execution(* org.example.scheduler.service.MeetingService.updateMeeting(..))")
    public void meetingUpdate() {}

    /**
     * Pointcut for meeting deletion methods.
     */
    @Pointcut("execution(* org.example.scheduler.service.MeetingService.deleteMeeting(..))")
    public void meetingDeletion() {}

    /**
     * Pointcut for meeting status change methods.
     */
    @Pointcut("execution(* org.example.scheduler.service.MeetingService.updateMeetingStatus(..))")
    public void meetingStatusChange() {}

    /**
     * After successful meeting creation, register with monitor.
     */
    @AfterReturning(pointcut = "meetingCreation()", returning = "result")
    public void afterMeetingCreation(JoinPoint joinPoint, Object result) {
        if (result instanceof SchedulingResultDTO dto && dto.isSuccess() && dto.getMeeting() != null) {
            log.debug("RV Aspect: Intercepted successful meeting creation for ID {}", 
                dto.getMeeting().getId());
            // Note: The actual monitoring is done in the service layer
            // This aspect logs and can add additional checks
        }
    }

    /**
     * Before meeting deletion, verify the meeting exists.
     */
    @Before("meetingDeletion() && args(meetingId)")
    public void beforeMeetingDeletion(JoinPoint joinPoint, Long meetingId) {
        log.debug("RV Aspect: Intercepted meeting deletion request for ID {}", meetingId);
        // Pre-deletion verification happens in the service
    }

    /**
     * After meeting status change, update monitor.
     */
    @AfterReturning(pointcut = "meetingStatusChange() && args(meetingId, newStatus)", returning = "result")
    public void afterStatusChange(JoinPoint joinPoint, Long meetingId, MeetingStatus newStatus, Object result) {
        log.debug("RV Aspect: Intercepted status change for meeting {} to {}", meetingId, newStatus);
        
        switch (newStatus) {
            case CONFIRMED -> meetingMonitor.onMeetingConfirm(meetingId);
            case REJECTED -> meetingMonitor.onMeetingReject(meetingId);
            case CANCELLED -> meetingMonitor.onMeetingCancel(meetingId, MeetingStatus.CONFIRMED);
            default -> log.debug("RV Aspect: No specific handling for status {}", newStatus);
        }
    }

    /**
     * Exception handling for meeting operations.
     */
    @AfterThrowing(pointcut = "meetingCreation() || meetingUpdate() || meetingDeletion()", 
                   throwing = "exception")
    public void afterMeetingOperationException(JoinPoint joinPoint, Exception exception) {
        log.warn("RV Aspect: Meeting operation failed - {}: {}", 
            joinPoint.getSignature().getName(), exception.getMessage());
    }
}

