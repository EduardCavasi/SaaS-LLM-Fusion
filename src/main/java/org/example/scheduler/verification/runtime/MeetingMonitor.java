package org.example.scheduler.verification.runtime;

import lombok.extern.slf4j.Slf4j;
import org.example.scheduler.model.Meeting;
import org.example.scheduler.model.MeetingStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime Verification Monitor for meeting scheduling.
 * 
 * Monitors the following LTL-like properties:
 * 
 * Property 1 - Create Flow: G (create(id) → F (confirm(id) ∨ reject(id)))
 *   Every created meeting must eventually be confirmed or rejected.
 * 
 * Property 2 - Delete Safety: G (delete(id) → previouslyCreated(id))
 *   Cannot delete a meeting that doesn't exist.
 * 
 * Property 3 - No Overlaps: G ¬overlaps(meetingA, meetingB)
 *   No two meetings can overlap in the same room.
 * 
 * Property 4 - Capacity: G (assign(room, attendees) → attendees ≤ capacity(room))
 *   Room capacity must not be exceeded.
 */
@Component
@Slf4j
public class MeetingMonitor {

    // Track pending meetings awaiting confirmation/rejection
    private final Map<Long, MeetingEvent> pendingMeetings = new ConcurrentHashMap<>();
    
    // Track all created meetings for delete safety check
    private final Set<Long> createdMeetings = ConcurrentHashMap.newKeySet();
    
    // Track active meetings per room for overlap detection
    private final Map<Long, List<ActiveMeetingSlot>> roomSchedule = new ConcurrentHashMap<>();
    
    // Event history for auditing
    private final List<MeetingEvent> eventHistory = Collections.synchronizedList(new ArrayList<>());
    
    // Detected violations
    private final List<PropertyViolation> violations = Collections.synchronizedList(new ArrayList<>());

    // Room capacities for capacity checking
    private final Map<Long, Integer> roomCapacities = new ConcurrentHashMap<>();

    /**
     * Record for tracking active meeting time slots.
     */
    private record ActiveMeetingSlot(
        Long meetingId,
        LocalDateTime startTime,
        LocalDateTime endTime
    ) {}

    /**
     * Registers a room's capacity for monitoring.
     */
    public void registerRoom(Long roomId, int capacity) {
        roomCapacities.put(roomId, capacity);
        roomSchedule.putIfAbsent(roomId, Collections.synchronizedList(new ArrayList<>()));
        log.debug("RV Monitor: Registered room {} with capacity {}", roomId, capacity);
    }

    /**
     * Called when a meeting is created.
     * Property 1: Starts tracking that this meeting must eventually be confirmed/rejected.
     */
    public List<PropertyViolation> onMeetingCreate(Meeting meeting) {
        List<PropertyViolation> newViolations = new ArrayList<>();
        
        MeetingEvent event = MeetingEvent.create(
            meeting.getId(),
            meeting.getRoom().getId(),
            meeting.getStartTime(),
            meeting.getEndTime(),
            meeting.getParticipants().size()
        );
        
        eventHistory.add(event);
        createdMeetings.add(meeting.getId());
        pendingMeetings.put(meeting.getId(), event);
        
        // Property 4: Check capacity constraint
        Integer roomCapacity = roomCapacities.get(meeting.getRoom().getId());
        if (roomCapacity != null && meeting.getParticipants().size() > roomCapacity) {
            PropertyViolation violation = PropertyViolation.error(
                "CAPACITY_EXCEEDED",
                "Room capacity exceeded",
                meeting.getId(),
                String.format("Meeting has %d participants but room capacity is %d",
                    meeting.getParticipants().size(), roomCapacity)
            );
            newViolations.add(violation);
            violations.add(violation);
        }
        
        // Property 3: Check for overlaps
        List<PropertyViolation> overlapViolations = checkOverlaps(meeting);
        newViolations.addAll(overlapViolations);
        violations.addAll(overlapViolations);
        
        // Add to room schedule if no violations
        if (overlapViolations.isEmpty()) {
            roomSchedule.computeIfAbsent(meeting.getRoom().getId(), 
                k -> Collections.synchronizedList(new ArrayList<>()))
                .add(new ActiveMeetingSlot(meeting.getId(), meeting.getStartTime(), meeting.getEndTime()));
        }
        
        log.info("RV Monitor: CREATE event for meeting {} - {} violations detected", 
            meeting.getId(), newViolations.size());
        
        return newViolations;
    }

    /**
     * Called when a meeting is confirmed.
     * Property 1: Satisfies the create → confirm requirement.
     */
    public List<PropertyViolation> onMeetingConfirm(Long meetingId) {
        List<PropertyViolation> newViolations = new ArrayList<>();
        
        MeetingEvent event = MeetingEvent.confirm(meetingId);
        eventHistory.add(event);
        
        MeetingEvent pending = pendingMeetings.remove(meetingId);
        if (pending == null) {
            PropertyViolation violation = PropertyViolation.warning(
                "CONFIRM_WITHOUT_CREATE",
                "Confirming a meeting that was not tracked as pending",
                meetingId,
                "Meeting may have been created before monitor was active"
            );
            newViolations.add(violation);
            violations.add(violation);
        }
        
        log.info("RV Monitor: CONFIRM event for meeting {}", meetingId);
        return newViolations;
    }

    /**
     * Called when a meeting is rejected.
     * Property 1: Satisfies the create → reject requirement.
     */
    public List<PropertyViolation> onMeetingReject(Long meetingId) {
        List<PropertyViolation> newViolations = new ArrayList<>();
        
        MeetingEvent event = MeetingEvent.reject(meetingId);
        eventHistory.add(event);
        
        pendingMeetings.remove(meetingId);
        
        // Remove from room schedule if present
        for (List<ActiveMeetingSlot> slots : roomSchedule.values()) {
            slots.removeIf(slot -> slot.meetingId().equals(meetingId));
        }
        
        log.info("RV Monitor: REJECT event for meeting {}", meetingId);
        return newViolations;
    }

    /**
     * Called when a meeting is deleted.
     * Property 2: Checks that the meeting was previously created.
     */
    public List<PropertyViolation> onMeetingDelete(Long meetingId, MeetingStatus previousStatus) {
        List<PropertyViolation> newViolations = new ArrayList<>();
        
        MeetingEvent event = MeetingEvent.delete(meetingId, previousStatus);
        eventHistory.add(event);
        
        // Property 2: Check delete safety
        if (!createdMeetings.contains(meetingId)) {
            PropertyViolation violation = PropertyViolation.error(
                "DELETE_NONEXISTENT",
                "Attempting to delete a meeting that doesn't exist",
                meetingId,
                "Property G(delete(id) → previouslyCreated(id)) violated"
            );
            newViolations.add(violation);
            violations.add(violation);
        }
        
        createdMeetings.remove(meetingId);
        pendingMeetings.remove(meetingId);
        
        // Remove from room schedule
        for (List<ActiveMeetingSlot> slots : roomSchedule.values()) {
            slots.removeIf(slot -> slot.meetingId().equals(meetingId));
        }
        
        log.info("RV Monitor: DELETE event for meeting {} - {} violations detected", 
            meetingId, newViolations.size());
        
        return newViolations;
    }

    /**
     * Called when a meeting is cancelled.
     */
    public List<PropertyViolation> onMeetingCancel(Long meetingId, MeetingStatus previousStatus) {
        List<PropertyViolation> newViolations = new ArrayList<>();
        
        MeetingEvent event = MeetingEvent.cancel(meetingId, previousStatus);
        eventHistory.add(event);
        
        pendingMeetings.remove(meetingId);
        
        // Remove from room schedule
        for (List<ActiveMeetingSlot> slots : roomSchedule.values()) {
            slots.removeIf(slot -> slot.meetingId().equals(meetingId));
        }
        
        log.info("RV Monitor: CANCEL event for meeting {}", meetingId);
        return newViolations;
    }

    /**
     * Checks Property 3: No overlapping meetings in the same room.
     */
    private List<PropertyViolation> checkOverlaps(Meeting newMeeting) {
        List<PropertyViolation> newViolations = new ArrayList<>();
        
        Long roomId = newMeeting.getRoom().getId();
        List<ActiveMeetingSlot> slots = roomSchedule.get(roomId);
        
        if (slots != null) {
            for (ActiveMeetingSlot existing : slots) {
                // Skip self-comparison
                if (existing.meetingId().equals(newMeeting.getId())) {
                    continue;
                }
                
                // Check for overlap: start1 < end2 AND start2 < end1
                if (newMeeting.getStartTime().isBefore(existing.endTime()) &&
                    existing.startTime().isBefore(newMeeting.getEndTime())) {
                    
                    PropertyViolation violation = PropertyViolation.critical(
                        "MEETING_OVERLAP",
                        "Overlapping meetings detected in same room",
                        newMeeting.getId(),
                        String.format("Meeting %d overlaps with meeting %d in room %d",
                            newMeeting.getId(), existing.meetingId(), roomId)
                    );
                    newViolations.add(violation);
                }
            }
        }
        
        return newViolations;
    }

    /**
     * Property 1 monitoring: Check for meetings that haven't been confirmed/rejected.
     * Should be called periodically or at system checkpoints.
     */
    public List<PropertyViolation> checkPendingMeetings() {
        List<PropertyViolation> newViolations = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<Long, MeetingEvent> entry : pendingMeetings.entrySet()) {
            MeetingEvent event = entry.getValue();
            
            // If meeting start time has passed and still pending, this is a violation
            if (event.startTime() != null && event.startTime().isBefore(now)) {
                PropertyViolation violation = PropertyViolation.error(
                    "UNRESOLVED_MEETING",
                    "Meeting started without being confirmed or rejected",
                    entry.getKey(),
                    String.format("Property G(create(id) → F(confirm(id) ∨ reject(id))) violated. " +
                        "Meeting created at %s, start time was %s",
                        event.eventTimestamp(), event.startTime())
                );
                newViolations.add(violation);
                violations.add(violation);
            }
        }
        
        return newViolations;
    }

    /**
     * Gets all detected violations.
     */
    public List<PropertyViolation> getViolations() {
        return new ArrayList<>(violations);
    }

    /**
     * Gets violations filtered by severity.
     */
    public List<PropertyViolation> getViolationsBySeverity(PropertyViolation.ViolationSeverity severity) {
        return violations.stream()
            .filter(v -> v.severity() == severity)
            .toList();
    }

    /**
     * Gets the event history.
     */
    public List<MeetingEvent> getEventHistory() {
        return new ArrayList<>(eventHistory);
    }

    /**
     * Gets pending meetings count.
     */
    public int getPendingMeetingsCount() {
        return pendingMeetings.size();
    }

    /**
     * Clears all monitoring state (for testing).
     */
    public void reset() {
        pendingMeetings.clear();
        createdMeetings.clear();
        roomSchedule.clear();
        eventHistory.clear();
        violations.clear();
        log.info("RV Monitor: State reset");
    }

    /**
     * Gets monitoring statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", eventHistory.size());
        stats.put("pendingMeetings", pendingMeetings.size());
        stats.put("trackedMeetings", createdMeetings.size());
        stats.put("totalViolations", violations.size());
        stats.put("criticalViolations", 
            getViolationsBySeverity(PropertyViolation.ViolationSeverity.CRITICAL).size());
        stats.put("errorViolations", 
            getViolationsBySeverity(PropertyViolation.ViolationSeverity.ERROR).size());
        stats.put("warningViolations", 
            getViolationsBySeverity(PropertyViolation.ViolationSeverity.WARNING).size());
        return stats;
    }
}

