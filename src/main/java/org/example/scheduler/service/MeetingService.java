package org.example.scheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduler.dto.AvailableSlotsRequestDTO;
import org.example.scheduler.dto.AvailableSlotsResponseDTO;
import org.example.scheduler.dto.BatchVerifyRequestDTO;
import org.example.scheduler.dto.MeetingDTO;
import org.example.scheduler.dto.ParticipantDTO;
import org.example.scheduler.dto.SchedulingResultDTO;
import org.example.scheduler.exception.ResourceNotFoundException;
import org.example.scheduler.exception.SchedulingException;
import org.example.scheduler.model.Meeting;
import org.example.scheduler.model.MeetingStatus;
import org.example.scheduler.model.Participant;
import org.example.scheduler.model.Room;
import org.example.scheduler.repository.MeetingRepository;
import org.example.scheduler.verification.runtime.MeetingMonitor;
import org.example.scheduler.verification.runtime.PropertyViolation;
import org.example.scheduler.verification.z3.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This service integrates:
 * - Z3 SMT Solver for static constraint validation before scheduling
 * - Runtime Verification for dynamic monitoring of meeting lifecycle
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final RoomService roomService;
    private final ParticipantService participantService;
    private final Z3ConstraintSolver constraintSolver;
    private final MeetingMonitor meetingMonitor;

    /**
     * Creates a new meeting with Z3 constraint verification.
     * 
     * 1. Validate input data
     * 2. Fetch existing meetings for constraint checking
     * 3. Use Z3 solver to verify scheduling feasibility
     * 4. If satisfiable, create the meeting
     * 5. Register with runtime monitor
     */
    @Transactional
    public SchedulingResultDTO createMeeting(MeetingDTO meetingDTO) {
        log.info("Creating meeting: {}", meetingDTO.getTitle());
        
        // Validate time range
        if (meetingDTO.getStartTime().isAfter(meetingDTO.getEndTime()) ||
            meetingDTO.getStartTime().equals(meetingDTO.getEndTime())) {
            return SchedulingResultDTO.failure(
                List.of("Start time must be before end time"),
                "Invalid time range",
                0L
            );
        }
        
        // Fetch room
        Room room = roomService.getRoomEntityById(meetingDTO.getRoomId());
        
        if (!room.getAvailable()) {
            return SchedulingResultDTO.failure(
                List.of("Room '" + room.getName() + "' is not available"),
                "Room unavailable",
                0L
            );
        }
        
        // Fetch participants
        Set<Participant> participants = participantService.getParticipantEntitiesByIds(meetingDTO.getParticipantIds());
        
        ConstraintResult result;
        
        if (constraintSolver.isEnabled()) {
            SchedulingConstraint newConstraint = new SchedulingConstraint(
                null,
                room.getId(),
                room.getCapacity(),
                meetingDTO.getStartTime(),
                meetingDTO.getEndTime(),
                meetingDTO.getParticipantIds()
            );
            
            List<ExistingMeeting> existingMeetings = getExistingMeetingsForConstraints();
            
            result = constraintSolver.checkSchedulingFeasibility(newConstraint, existingMeetings);
            
            if (!result.satisfiable()) {
                log.warn("Z3 Solver: Meeting scheduling is UNSATISFIABLE - {} violations", 
                    result.violations().size());
                return SchedulingResultDTO.failure(
                    result.violations(),
                    "Scheduling constraints cannot be satisfied",
                    result.solvingTimeMs()
                );
            }
        } else {
            result = ConstraintResult.success(0);
        }
        
        // Create the meeting
        Meeting meeting = Meeting.builder()
                .title(meetingDTO.getTitle())
                .description(meetingDTO.getDescription())
                .startTime(meetingDTO.getStartTime())
                .endTime(meetingDTO.getEndTime())
                .room(room)
                .participants(participants)
                .status(MeetingStatus.PENDING)
                .build();
        
        meeting = meetingRepository.save(meeting);
        
        // Runtime Verification: Register meeting creation
        List<PropertyViolation> rvViolations = meetingMonitor.onMeetingCreate(meeting);
        
        List<String> warnings = rvViolations.stream()
                .map(v -> v.propertyName() + ": " + v.description())
                .collect(Collectors.toList());
        
        MeetingDTO resultDTO = toDTO(meeting);
        
        SchedulingResultDTO schedulingResult = SchedulingResultDTO.success(
            resultDTO,
            "Meeting scheduled successfully",
            result.solvingTimeMs()
        );
        schedulingResult.setRuntimeWarnings(warnings);
        
        meetingMonitor.checkPendingMeetings();
        
        log.info("Created meeting with ID: {} (Z3 solving took {}ms)", 
            meeting.getId(), result.solvingTimeMs());
        
        return schedulingResult;
    }

    /**
     * Gets a meeting by ID.
     */
    @Transactional(readOnly = true)
    public MeetingDTO getMeetingById(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", id));
        return toDTO(meeting);
    }

    /**
     * Gets all meetings.
     */
    @Transactional(readOnly = true)
    public List<MeetingDTO> getAllMeetings() {
        return meetingRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets meetings by status.
     */
    @Transactional(readOnly = true)
    public List<MeetingDTO> getMeetingsByStatus(MeetingStatus status) {
        return meetingRepository.findByStatus(status).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets meetings in a room.
     */
    @Transactional(readOnly = true)
    public List<MeetingDTO> getMeetingsByRoom(Long roomId) {
        return meetingRepository.findByRoomId(roomId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets meetings in a time range.
     */
    @Transactional(readOnly = true)
    public List<MeetingDTO> getMeetingsInTimeRange(LocalDateTime start, LocalDateTime end) {
        return meetingRepository.findMeetingsInTimeRange(start, end).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates a meeting with Z3 constraint verification.
     */
    @Transactional
    public SchedulingResultDTO updateMeeting(Long id, MeetingDTO meetingDTO) {
        log.info("Updating meeting ID: {}", id);
        
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", id));
        
        // Cannot update completed or cancelled meetings
        if (meeting.getStatus() == MeetingStatus.COMPLETED || 
            meeting.getStatus() == MeetingStatus.CANCELLED) {
            return SchedulingResultDTO.failure(
                List.of("Cannot update a " + meeting.getStatus().name().toLowerCase() + " meeting"),
                "Invalid meeting status for update",
                0L
            );
        }
        
        // Prepare updated values
        LocalDateTime newStartTime = meetingDTO.getStartTime() != null ? 
            meetingDTO.getStartTime() : meeting.getStartTime();
        LocalDateTime newEndTime = meetingDTO.getEndTime() != null ? 
            meetingDTO.getEndTime() : meeting.getEndTime();
        Long newRoomId = meetingDTO.getRoomId() != null ? 
            meetingDTO.getRoomId() : meeting.getRoom().getId();
        Set<Long> newParticipantIds = meetingDTO.getParticipantIds() != null ? 
            meetingDTO.getParticipantIds() : 
            meeting.getParticipants().stream().map(Participant::getId).collect(Collectors.toSet());
        
        // Fetch room
        Room room = roomService.getRoomEntityById(newRoomId);
        
        ConstraintResult result;
        
        if (constraintSolver.isEnabled()) {
            SchedulingConstraint updateConstraint = new SchedulingConstraint(
                id,
                room.getId(),
                room.getCapacity(),
                newStartTime,
                newEndTime,
                newParticipantIds
            );
            
            List<ExistingMeeting> existingMeetings = getExistingMeetingsForConstraints();
            
            result = constraintSolver.checkSchedulingFeasibility(updateConstraint, existingMeetings);
            
            if (!result.satisfiable()) {
                log.warn("Z3 Solver: Meeting update is UNSATISFIABLE - {} violations", 
                    result.violations().size());
                return SchedulingResultDTO.failure(
                    result.violations(),
                    "Updated scheduling constraints cannot be satisfied",
                    result.solvingTimeMs()
                );
            }
        } else {
            result = ConstraintResult.success(0);
        }
        
        // Apply updates
        if (meetingDTO.getTitle() != null) {
            meeting.setTitle(meetingDTO.getTitle());
        }
        if (meetingDTO.getDescription() != null) {
            meeting.setDescription(meetingDTO.getDescription());
        }
        meeting.setStartTime(newStartTime);
        meeting.setEndTime(newEndTime);
        meeting.setRoom(room);
        
        if (meetingDTO.getParticipantIds() != null) {
            Set<Participant> participants = participantService.getParticipantEntitiesByIds(newParticipantIds);
            meeting.setParticipants(participants);
        }
        
        meeting = meetingRepository.save(meeting);
        
        meetingMonitor.checkPendingMeetings();
        
        MeetingDTO resultDTO = toDTO(meeting);
        
        log.info("Updated meeting ID: {} (Z3 solving took {}ms)", id, result.solvingTimeMs());
        
        return SchedulingResultDTO.success(
            resultDTO,
            "Meeting updated successfully",
            result.solvingTimeMs()
        );
    }

    /**
     * Updates meeting status.
     */
    @Transactional
    public MeetingDTO updateMeetingStatus(Long id, MeetingStatus newStatus) {
        log.info("Updating meeting {} status to {}", id, newStatus);
        
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", id));
        
        MeetingStatus oldStatus = meeting.getStatus();
        
        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);
        
        meeting.setStatus(newStatus);
        meeting = meetingRepository.save(meeting);
        
        switch (newStatus) {
            case CONFIRMED -> meetingMonitor.onMeetingConfirm(id);
            case REJECTED -> meetingMonitor.onMeetingReject(id);
            case CANCELLED -> meetingMonitor.onMeetingCancel(id, oldStatus);
        }
        
        meetingMonitor.checkPendingMeetings();
        
        log.info("Updated meeting {} status from {} to {}", id, oldStatus, newStatus);
        
        return toDTO(meeting);
    }

    /**
     * Deletes a meeting.
     */
    @Transactional
    public void deleteMeeting(Long id) {
        log.info("Deleting meeting ID: {}", id);
        
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", id));
        
        MeetingStatus previousStatus = meeting.getStatus();
        
        List<PropertyViolation> violations = meetingMonitor.onMeetingDelete(id, previousStatus);
        
        if (!violations.isEmpty()) {
            List<String> violationMessages = violations.stream()
                    .filter(v -> v.severity() == PropertyViolation.ViolationSeverity.ERROR ||
                                 v.severity() == PropertyViolation.ViolationSeverity.CRITICAL)
                    .map(PropertyViolation::description)
                    .collect(Collectors.toList());
            
            if (!violationMessages.isEmpty()) {
                throw new SchedulingException("Delete operation violates runtime properties", violationMessages);
            }
        }
        
        meetingRepository.delete(meeting);
        
        meetingMonitor.removeViolationsForMeeting(id);
        meetingMonitor.checkPendingMeetings();
        
        log.info("Deleted meeting ID: {}", id);
    }

    /**
     * Confirms a pending meeting.
     */
    @Transactional
    public MeetingDTO confirmMeeting(Long id) {
        return updateMeetingStatus(id, MeetingStatus.CONFIRMED);
    }

    /**
     * Rejects a pending meeting.
     */
    @Transactional
    public MeetingDTO rejectMeeting(Long id) {
        return updateMeetingStatus(id, MeetingStatus.REJECTED);
    }

    /**
     * Cancels a meeting.
     */
    @Transactional
    public MeetingDTO cancelMeeting(Long id) {
        return updateMeetingStatus(id, MeetingStatus.CANCELLED);
    }

    /**
     * Gets runtime verification statistics.
     */
    public Map<String, Object> getVerificationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("z3SolverInitialized", constraintSolver.isInitialized());
        stats.put("z3SolverEnabled", constraintSolver.isEnabled());
        stats.putAll(meetingMonitor.getStatistics());
        return stats;
    }

    public boolean isZ3SolverEnabled() {
        return constraintSolver.isEnabled();
    }

    public void setZ3SolverEnabled(boolean enabled) {
        constraintSolver.setEnabled(enabled);
    }

    public AvailableSlotsResponseDTO findAvailableSlots(AvailableSlotsRequestDTO request) {
        if (!constraintSolver.isEnabled()) {
            throw new IllegalStateException("Z3 Solver is disabled");
        }

        List<ExistingMeeting> existingMeetings = getExistingMeetingsForConstraints();
        
        long searchStartEpoch = request.getSearchStart().atZone(ZoneId.systemDefault()).toEpochSecond();
        long searchEndEpoch = request.getSearchEnd().atZone(ZoneId.systemDefault()).toEpochSecond();
        
        List<Long> availableSlotEpochs = constraintSolver.findAvailableSlots(
            request.getRoomId(),
            request.getDurationMinutes(),
            searchStartEpoch,
            searchEndEpoch,
            existingMeetings
        );
        
        List<LocalDateTime> availableSlots = availableSlotEpochs.stream()
            .map(epoch -> LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault()))
            .collect(Collectors.toList());
        
        return AvailableSlotsResponseDTO.builder()
            .roomId(request.getRoomId())
            .durationMinutes(request.getDurationMinutes())
            .searchStart(request.getSearchStart())
            .searchEnd(request.getSearchEnd())
            .availableSlots(availableSlots)
            .totalSlots(availableSlots.size())
            .build();
    }

    public SchedulingResultDTO verifyBatchScheduling(List<BatchVerifyRequestDTO> meetings) {
        if (!constraintSolver.isEnabled()) {
            throw new IllegalStateException("Z3 Solver is disabled");
        }

        List<ExistingMeeting> existingMeetings = getExistingMeetingsForConstraints();
        
        List<SchedulingConstraint> constraints = new ArrayList<>();
        
        for (BatchVerifyRequestDTO meetingDTO : meetings) {
            Room room = roomService.getRoomEntityById(meetingDTO.getRoomId());
            
            SchedulingConstraint constraint = new SchedulingConstraint(
                null,
                room.getId(),
                room.getCapacity(),
                meetingDTO.getStartTime(),
                meetingDTO.getEndTime(),
                meetingDTO.getParticipantIds()
            );
            constraints.add(constraint);
        }
        
        ConstraintResult result = constraintSolver.verifyBatchScheduling(constraints, existingMeetings);
        
        if (result.satisfiable()) {
            return SchedulingResultDTO.success(
                null,
                String.format("Batch verification successful: %d meetings can be scheduled", meetings.size()),
                result.solvingTimeMs()
            );
        } else {
            return SchedulingResultDTO.failure(
                result.violations(),
                "Batch scheduling constraints cannot be satisfied",
                result.solvingTimeMs()
            );
        }
    }

    /**
     * Gets runtime violations.
     */
    public List<PropertyViolation> getRuntimeViolations() {
        return meetingMonitor.getViolations();
    }

    /**
     * Checks pending meetings for property violations.
     */
    public List<PropertyViolation> checkPendingMeetingsCompliance() {
        return meetingMonitor.checkPendingMeetings();
    }

    /**
     * Builds a list of existing meetings for Z3 constraint checking.
     */
    private List<ExistingMeeting> getExistingMeetingsForConstraints() {
        return meetingRepository.findByStatus(MeetingStatus.CONFIRMED).stream()
                .map(m -> new ExistingMeeting(
                    m.getId(),
                    m.getRoom().getId(),
                    m.getStartTime(),
                    m.getEndTime(),
                    m.getParticipants().stream()
                        .map(Participant::getId)
                        .collect(Collectors.toSet())
                ))
                .collect(Collectors.toList());
    }

    /**
     * Validates meeting status transitions.
     */
    private void validateStatusTransition(MeetingStatus from, MeetingStatus to) {
        boolean valid = switch (from) {
            case PENDING -> to == MeetingStatus.CONFIRMED || to == MeetingStatus.REJECTED;
            case CONFIRMED -> to == MeetingStatus.CANCELLED || to == MeetingStatus.COMPLETED;
            case REJECTED, CANCELLED, COMPLETED -> false;
        };
        
        if (!valid) {
            throw new IllegalArgumentException(
                String.format("Invalid status transition from %s to %s", from, to));
        }
    }

    /**
     * Converts Meeting entity to DTO.
     */
    private MeetingDTO toDTO(Meeting meeting) {
        Set<ParticipantDTO> participantDTOs = meeting.getParticipants().stream()
                .map(p -> ParticipantDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .email(p.getEmail())
                        .department(p.getDepartment())
                        .build())
                .collect(Collectors.toSet());
        
        return MeetingDTO.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .roomId(meeting.getRoom().getId())
                .roomName(meeting.getRoom().getName())
                .participantIds(meeting.getParticipants().stream()
                        .map(Participant::getId)
                        .collect(Collectors.toSet()))
                .participants(participantDTOs)
                .status(meeting.getStatus())
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }
}

