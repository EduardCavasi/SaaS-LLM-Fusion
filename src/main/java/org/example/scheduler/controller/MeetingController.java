package org.example.scheduler.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.scheduler.dto.MeetingDTO;
import org.example.scheduler.dto.SchedulingResultDTO;
import org.example.scheduler.model.MeetingStatus;
import org.example.scheduler.service.MeetingService;
import org.example.scheduler.verification.runtime.PropertyViolation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing meetings.
 * 
 * This controller provides endpoints for:
 * - CRUD operations on meetings
 * - Meeting status management (confirm, reject, cancel)
 * - Z3-verified scheduling
 * - Runtime verification monitoring
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    /**
     * Creates a new meeting with Z3 constraint verification.
     * 
     * The Z3 solver checks:
     * - No room conflicts (overlapping meetings in same room)
     * - No participant conflicts (double-booking participants)
     * - Room capacity constraints
     */
    @PostMapping
    public ResponseEntity<SchedulingResultDTO> createMeeting(@Valid @RequestBody MeetingDTO meetingDTO) {
        SchedulingResultDTO result = meetingService.createMeeting(meetingDTO);
        
        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
    }

    /**
     * Gets a meeting by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingDTO> getMeetingById(@PathVariable Long id) {
        MeetingDTO meeting = meetingService.getMeetingById(id);
        return ResponseEntity.ok(meeting);
    }

    /**
     * Gets all meetings.
     */
    @GetMapping
    public ResponseEntity<List<MeetingDTO>> getAllMeetings() {
        List<MeetingDTO> meetings = meetingService.getAllMeetings();
        return ResponseEntity.ok(meetings);
    }

    /**
     * Gets meetings by status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<MeetingDTO>> getMeetingsByStatus(@PathVariable MeetingStatus status) {
        List<MeetingDTO> meetings = meetingService.getMeetingsByStatus(status);
        return ResponseEntity.ok(meetings);
    }

    /**
     * Gets meetings in a specific room.
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<MeetingDTO>> getMeetingsByRoom(@PathVariable Long roomId) {
        List<MeetingDTO> meetings = meetingService.getMeetingsByRoom(roomId);
        return ResponseEntity.ok(meetings);
    }

    /**
     * Gets meetings in a time range.
     */
    @GetMapping("/range")
    public ResponseEntity<List<MeetingDTO>> getMeetingsInTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<MeetingDTO> meetings = meetingService.getMeetingsInTimeRange(start, end);
        return ResponseEntity.ok(meetings);
    }

    /**
     * Updates a meeting with Z3 constraint verification.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SchedulingResultDTO> updateMeeting(
            @PathVariable Long id, 
            @RequestBody MeetingDTO meetingDTO) {
        SchedulingResultDTO result = meetingService.updateMeeting(id, meetingDTO);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
    }

    /**
     * Deletes a meeting.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long id) {
        meetingService.deleteMeeting(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Confirms a pending meeting.
     * Property: Satisfies G(create → F(confirm ∨ reject))
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<MeetingDTO> confirmMeeting(@PathVariable Long id) {
        MeetingDTO meeting = meetingService.confirmMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    /**
     * Rejects a pending meeting.
     * Property: Satisfies G(create → F(confirm ∨ reject))
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<MeetingDTO> rejectMeeting(@PathVariable Long id) {
        MeetingDTO meeting = meetingService.rejectMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    /**
     * Cancels a confirmed meeting.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<MeetingDTO> cancelMeeting(@PathVariable Long id) {
        MeetingDTO meeting = meetingService.cancelMeeting(id);
        return ResponseEntity.ok(meeting);
    }

    /**
     * Gets verification statistics including Z3 and runtime verification status.
     */
    @GetMapping("/verification/stats")
    public ResponseEntity<Map<String, Object>> getVerificationStatistics() {
        Map<String, Object> stats = meetingService.getVerificationStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets runtime verification violations.
     */
    @GetMapping("/verification/violations")
    public ResponseEntity<List<PropertyViolation>> getRuntimeViolations() {
        List<PropertyViolation> violations = meetingService.getRuntimeViolations();
        return ResponseEntity.ok(violations);
    }

    /**
     * Checks pending meetings for compliance with temporal properties.
     * Property: G(create → F(confirm ∨ reject))
     */
    @PostMapping("/verification/check-pending")
    public ResponseEntity<List<PropertyViolation>> checkPendingMeetings() {
        List<PropertyViolation> violations = meetingService.checkPendingMeetingsCompliance();
        return ResponseEntity.ok(violations);
    }
}

