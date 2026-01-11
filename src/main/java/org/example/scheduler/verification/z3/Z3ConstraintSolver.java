package org.example.scheduler.verification.z3;

import com.microsoft.z3.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Z3 SMT Solver for meeting scheduling constraint verification.
 * 
 * This component uses Z3 to verify scheduling feasibility by checking:
 * 1. No overlapping meetings in the same room
 * 2. All required participants must be free (no double-booking)
 * 3. Room capacity must not be exceeded
 * 
 * The solver encodes these constraints as SMT formulas and checks satisfiability.
 */
@Component
@Slf4j
public class Z3ConstraintSolver {

    private Context ctx;
    private boolean initialized = false;

    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing Z3 Constraint Solver...");
            
            // Configure Z3 context
            Map<String, String> cfg = new HashMap<>();
            cfg.put("model", "true");
            cfg.put("proof", "false");
            
            ctx = new Context(cfg);
            initialized = true;
            
            log.info("Z3 Constraint Solver initialized successfully. Version: {}", Version.getString());
        } catch (Exception e) {
            log.error("Failed to initialize Z3 Constraint Solver", e);
            initialized = false;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (ctx != null) {
            ctx.close();
            log.info("Z3 Context closed");
        }
    }

    /**
     * Checks if a new meeting can be scheduled without violating constraints.
     *
     * @param newMeeting The new meeting constraint to check
     * @param existingMeetings List of existing meetings in the system
     * @return ConstraintResult indicating whether scheduling is feasible
     */
    public ConstraintResult checkSchedulingFeasibility(
            SchedulingConstraint newMeeting,
            List<ExistingMeeting> existingMeetings) {
        
        if (!initialized) {
            return ConstraintResult.error("Z3 Solver not initialized", 0);
        }

        long startTime = System.currentTimeMillis();
        List<String> violations = new ArrayList<>();

        try {
            Solver solver = ctx.mkSolver();

            // === Pre-validation checks ===
            
            // Check valid time range
            if (!newMeeting.hasValidTimeRange()) {
                violations.add("Invalid time range: start time must be before end time");
                return ConstraintResult.failure(violations, System.currentTimeMillis() - startTime);
            }

            // Check room capacity
            if (!newMeeting.fitsCapacity()) {
                violations.add(String.format(
                    "Room capacity exceeded: %d participants requested, but room capacity is %d",
                    newMeeting.participantIds().size(),
                    newMeeting.roomCapacity()
                ));
                return ConstraintResult.failure(violations, System.currentTimeMillis() - startTime);
            }

            // === Z3 Constraint Encoding ===

            // Create integer constants for the new meeting
            IntExpr newStart = ctx.mkInt(newMeeting.getStartEpochSecond());
            IntExpr newEnd = ctx.mkInt(newMeeting.getEndEpochSecond());
            IntExpr newRoom = ctx.mkInt(newMeeting.roomId());

            // Check for room conflicts with existing meetings
            for (ExistingMeeting existing : existingMeetings) {
                // Skip if checking against itself (for updates)
                if (newMeeting.meetingId() != null && 
                    newMeeting.meetingId().equals(existing.meetingId())) {
                    continue;
                }

                IntExpr existingStart = ctx.mkInt(existing.getStartEpochSecond());
                IntExpr existingEnd = ctx.mkInt(existing.getEndEpochSecond());
                IntExpr existingRoom = ctx.mkInt(existing.roomId());

                // Constraint: No overlapping meetings in the same room
                // Overlap occurs when: room1 == room2 AND start1 < end2 AND start2 < end1
                BoolExpr sameRoom = ctx.mkEq(newRoom, existingRoom);
                BoolExpr overlaps = ctx.mkAnd(
                    ctx.mkLt(newStart, existingEnd),
                    ctx.mkLt(existingStart, newEnd)
                );
                
                // If same room and overlaps, this is a conflict
                BoolExpr roomConflict = ctx.mkAnd(sameRoom, overlaps);
                
                // We assert that there should be NO room conflict
                // If the solver finds this unsatisfiable, there's a conflict
                solver.push();
                solver.add(ctx.mkNot(roomConflict));
                
                if (solver.check() == Status.UNSATISFIABLE) {
                    violations.add(String.format(
                        "Room conflict: Meeting overlaps with existing meeting ID %d in room %d (from %s to %s)",
                        existing.meetingId(),
                        existing.roomId(),
                        existing.startTime(),
                        existing.endTime()
                    ));
                }
                solver.pop();
            }

            // Check for participant conflicts
            for (Long participantId : newMeeting.participantIds()) {
                for (ExistingMeeting existing : existingMeetings) {
                    // Skip if checking against itself
                    if (newMeeting.meetingId() != null && 
                        newMeeting.meetingId().equals(existing.meetingId())) {
                        continue;
                    }

                    // Check if participant is in the existing meeting
                    if (existing.involvesParticipant(participantId)) {
                        IntExpr existingStart = ctx.mkInt(existing.getStartEpochSecond());
                        IntExpr existingEnd = ctx.mkInt(existing.getEndEpochSecond());

                        // Check for time overlap
                        BoolExpr participantOverlap = ctx.mkAnd(
                            ctx.mkLt(newStart, existingEnd),
                            ctx.mkLt(existingStart, newEnd)
                        );

                        solver.push();
                        solver.add(ctx.mkNot(participantOverlap));
                        
                        if (solver.check() == Status.UNSATISFIABLE) {
                            violations.add(String.format(
                                "Participant conflict: Participant ID %d is already booked for meeting ID %d (from %s to %s)",
                                participantId,
                                existing.meetingId(),
                                existing.startTime(),
                                existing.endTime()
                            ));
                        }
                        solver.pop();
                    }
                }
            }

            long solvingTime = System.currentTimeMillis() - startTime;

            if (violations.isEmpty()) {
                log.info("Z3 Solver: Scheduling is SATISFIABLE ({}ms)", solvingTime);
                return ConstraintResult.success(solvingTime);
            } else {
                log.info("Z3 Solver: Scheduling is UNSATISFIABLE - {} violations found ({}ms)", 
                        violations.size(), solvingTime);
                return ConstraintResult.failure(violations, solvingTime);
            }

        } catch (Exception e) {
            log.error("Z3 Solver error", e);
            return ConstraintResult.error("Solver error: " + e.getMessage(), 
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Finds available time slots for a meeting in a given room.
     *
     * @param roomId The room to check
     * @param duration Duration in minutes
     * @param searchStart Start of search window
     * @param searchEnd End of search window
     * @param existingMeetings Existing meetings to avoid
     * @return List of available time slot start times
     */
    public List<Long> findAvailableSlots(
            Long roomId,
            int durationMinutes,
            long searchStart,
            long searchEnd,
            List<ExistingMeeting> existingMeetings) {

        List<Long> availableSlots = new ArrayList<>();
        long durationSeconds = durationMinutes * 60L;
        long slotIncrement = 15 * 60L; // Check every 15 minutes

        // Filter meetings for this room
        List<ExistingMeeting> roomMeetings = existingMeetings.stream()
                .filter(m -> m.roomId().equals(roomId))
                .sorted(Comparator.comparing(ExistingMeeting::getStartEpochSecond))
                .toList();

        for (long slotStart = searchStart; slotStart + durationSeconds <= searchEnd; slotStart += slotIncrement) {
            long slotEnd = slotStart + durationSeconds;
            boolean isAvailable = true;

            for (ExistingMeeting meeting : roomMeetings) {
                // Check for overlap
                if (slotStart < meeting.getEndEpochSecond() && meeting.getStartEpochSecond() < slotEnd) {
                    isAvailable = false;
                    // Skip to end of this meeting
                    slotStart = meeting.getEndEpochSecond() - slotIncrement;
                    break;
                }
            }

            if (isAvailable) {
                availableSlots.add(slotStart);
            }
        }

        return availableSlots;
    }

    /**
     * Verifies a batch of scheduling constraints atomically.
     *
     * @param meetings List of meeting constraints to verify together
     * @param existingMeetings Existing meetings in the system
     * @return ConstraintResult for the batch operation
     */
    public ConstraintResult verifyBatchScheduling(
            List<SchedulingConstraint> meetings,
            List<ExistingMeeting> existingMeetings) {

        if (!initialized) {
            return ConstraintResult.error("Z3 Solver not initialized", 0);
        }

        long startTime = System.currentTimeMillis();
        List<String> violations = new ArrayList<>();

        try {
            Solver solver = ctx.mkSolver();

            // Check each new meeting against existing meetings
            for (SchedulingConstraint newMeeting : meetings) {
                ConstraintResult result = checkSchedulingFeasibility(newMeeting, existingMeetings);
                if (!result.satisfiable()) {
                    violations.addAll(result.violations());
                }
            }

            // Check new meetings against each other
            for (int i = 0; i < meetings.size(); i++) {
                for (int j = i + 1; j < meetings.size(); j++) {
                    SchedulingConstraint m1 = meetings.get(i);
                    SchedulingConstraint m2 = meetings.get(j);

                    // Check room conflict between new meetings
                    if (m1.roomId().equals(m2.roomId())) {
                        IntExpr start1 = ctx.mkInt(m1.getStartEpochSecond());
                        IntExpr end1 = ctx.mkInt(m1.getEndEpochSecond());
                        IntExpr start2 = ctx.mkInt(m2.getStartEpochSecond());
                        IntExpr end2 = ctx.mkInt(m2.getEndEpochSecond());

                        BoolExpr overlaps = ctx.mkAnd(
                            ctx.mkLt(start1, end2),
                            ctx.mkLt(start2, end1)
                        );

                        solver.push();
                        solver.add(ctx.mkNot(overlaps));
                        
                        if (solver.check() == Status.UNSATISFIABLE) {
                            violations.add(String.format(
                                "Batch conflict: New meetings at indices %d and %d overlap in room %d",
                                i, j, m1.roomId()
                            ));
                        }
                        solver.pop();
                    }

                    // Check participant conflicts between new meetings
                    Set<Long> commonParticipants = new HashSet<>(m1.participantIds());
                    commonParticipants.retainAll(m2.participantIds());

                    if (!commonParticipants.isEmpty()) {
                        IntExpr start1 = ctx.mkInt(m1.getStartEpochSecond());
                        IntExpr end1 = ctx.mkInt(m1.getEndEpochSecond());
                        IntExpr start2 = ctx.mkInt(m2.getStartEpochSecond());
                        IntExpr end2 = ctx.mkInt(m2.getEndEpochSecond());

                        BoolExpr overlaps = ctx.mkAnd(
                            ctx.mkLt(start1, end2),
                            ctx.mkLt(start2, end1)
                        );

                        solver.push();
                        solver.add(ctx.mkNot(overlaps));
                        
                        if (solver.check() == Status.UNSATISFIABLE) {
                            violations.add(String.format(
                                "Batch conflict: Participants %s are double-booked between meetings at indices %d and %d",
                                commonParticipants, i, j
                            ));
                        }
                        solver.pop();
                    }
                }
            }

            long solvingTime = System.currentTimeMillis() - startTime;

            if (violations.isEmpty()) {
                return ConstraintResult.success(solvingTime);
            } else {
                return ConstraintResult.failure(violations, solvingTime);
            }

        } catch (Exception e) {
            log.error("Z3 Batch verification error", e);
            return ConstraintResult.error("Batch solver error: " + e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}

