package org.example.scheduler.service;

import org.example.scheduler.dto.MeetingDTO;
import org.example.scheduler.dto.ParticipantDTO;
import org.example.scheduler.dto.RoomDTO;
import org.example.scheduler.dto.SchedulingResultDTO;
import org.example.scheduler.model.MeetingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MeetingService with Z3 constraint verification.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MeetingServiceTest {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ParticipantService participantService;

    private RoomDTO testRoom;
    private ParticipantDTO participant1;
    private ParticipantDTO participant2;

    @BeforeEach
    void setUp() {
        // Create test room
        testRoom = roomService.createRoom(RoomDTO.builder()
                .name("Test Room")
                .capacity(10)
                .location("Test Location")
                .available(true)
                .build());

        // Create test participants
        participant1 = participantService.createParticipant(ParticipantDTO.builder()
                .name("Test User 1")
                .email("test1@test.com")
                .department("Test")
                .build());

        participant2 = participantService.createParticipant(ParticipantDTO.builder()
                .name("Test User 2")
                .email("test2@test.com")
                .department("Test")
                .build());
    }

    @Test
    @DisplayName("Should create meeting when no conflicts exist")
    void testCreateMeeting_Success() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(1);

        MeetingDTO meetingDTO = MeetingDTO.builder()
                .title("Test Meeting")
                .description("Test Description")
                .startTime(start)
                .endTime(end)
                .roomId(testRoom.getId())
                .participantIds(Set.of(participant1.getId(), participant2.getId()))
                .build();

        SchedulingResultDTO result = meetingService.createMeeting(meetingDTO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getMeeting());
        assertEquals("Test Meeting", result.getMeeting().getTitle());
        assertEquals(MeetingStatus.PENDING, result.getMeeting().getStatus());
        assertEquals("SATISFIABLE", result.getSolverStatus());
    }

    @Test
    @DisplayName("Should reject meeting when room conflict exists")
    void testCreateMeeting_RoomConflict() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(1);

        // Create first meeting
        MeetingDTO meeting1 = MeetingDTO.builder()
                .title("First Meeting")
                .startTime(start)
                .endTime(end)
                .roomId(testRoom.getId())
                .participantIds(Set.of(participant1.getId()))
                .build();

        SchedulingResultDTO result1 = meetingService.createMeeting(meeting1);
        assertTrue(result1.isSuccess());
        
        // Confirm the first meeting
        meetingService.confirmMeeting(result1.getMeeting().getId());

        // Try to create overlapping meeting in same room
        MeetingDTO meeting2 = MeetingDTO.builder()
                .title("Second Meeting")
                .startTime(start.plusMinutes(30))
                .endTime(end.plusMinutes(30))
                .roomId(testRoom.getId())
                .participantIds(Set.of(participant2.getId()))
                .build();

        SchedulingResultDTO result2 = meetingService.createMeeting(meeting2);

        assertFalse(result2.isSuccess());
        assertEquals("UNSATISFIABLE", result2.getSolverStatus());
        assertFalse(result2.getConstraintViolations().isEmpty());
    }

    @Test
    @DisplayName("Should reject meeting when participant has conflict")
    void testCreateMeeting_ParticipantConflict() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(1);

        // Create another room
        RoomDTO room2 = roomService.createRoom(RoomDTO.builder()
                .name("Room 2")
                .capacity(5)
                .available(true)
                .build());

        // Create first meeting with participant1
        MeetingDTO meeting1 = MeetingDTO.builder()
                .title("First Meeting")
                .startTime(start)
                .endTime(end)
                .roomId(testRoom.getId())
                .participantIds(Set.of(participant1.getId()))
                .build();

        SchedulingResultDTO result1 = meetingService.createMeeting(meeting1);
        assertTrue(result1.isSuccess());
        
        // Confirm the meeting
        meetingService.confirmMeeting(result1.getMeeting().getId());

        // Try to create overlapping meeting with same participant in different room
        MeetingDTO meeting2 = MeetingDTO.builder()
                .title("Second Meeting")
                .startTime(start.plusMinutes(30))
                .endTime(end.plusMinutes(30))
                .roomId(room2.getId())
                .participantIds(Set.of(participant1.getId()))
                .build();

        SchedulingResultDTO result2 = meetingService.createMeeting(meeting2);

        assertFalse(result2.isSuccess());
        assertTrue(result2.getConstraintViolations().stream()
                .anyMatch(v -> v.contains("Participant conflict")));
    }

    @Test
    @DisplayName("Should reject meeting when room capacity exceeded")
    void testCreateMeeting_CapacityExceeded() {
        // Create a small room
        RoomDTO smallRoom = roomService.createRoom(RoomDTO.builder()
                .name("Small Room")
                .capacity(1)
                .available(true)
                .build());

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(1);

        MeetingDTO meeting = MeetingDTO.builder()
                .title("Too Many Participants")
                .startTime(start)
                .endTime(end)
                .roomId(smallRoom.getId())
                .participantIds(Set.of(participant1.getId(), participant2.getId()))
                .build();

        SchedulingResultDTO result = meetingService.createMeeting(meeting);

        assertFalse(result.isSuccess());
        assertTrue(result.getConstraintViolations().stream()
                .anyMatch(v -> v.contains("capacity")));
    }

    @Test
    @DisplayName("Should confirm pending meeting")
    void testConfirmMeeting() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(1);

        MeetingDTO meetingDTO = MeetingDTO.builder()
                .title("Meeting to Confirm")
                .startTime(start)
                .endTime(end)
                .roomId(testRoom.getId())
                .participantIds(Set.of(participant1.getId()))
                .build();

        SchedulingResultDTO result = meetingService.createMeeting(meetingDTO);
        assertTrue(result.isSuccess());
        assertEquals(MeetingStatus.PENDING, result.getMeeting().getStatus());

        MeetingDTO confirmed = meetingService.confirmMeeting(result.getMeeting().getId());
        assertEquals(MeetingStatus.CONFIRMED, confirmed.getStatus());
    }

    @Test
    @DisplayName("Should reject pending meeting")
    void testRejectMeeting() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(1);

        MeetingDTO meetingDTO = MeetingDTO.builder()
                .title("Meeting to Reject")
                .startTime(start)
                .endTime(end)
                .roomId(testRoom.getId())
                .participantIds(Set.of(participant1.getId()))
                .build();

        SchedulingResultDTO result = meetingService.createMeeting(meetingDTO);
        assertTrue(result.isSuccess());

        MeetingDTO rejected = meetingService.rejectMeeting(result.getMeeting().getId());
        assertEquals(MeetingStatus.REJECTED, rejected.getStatus());
    }

    @Test
    @DisplayName("Should report verification statistics")
    void testVerificationStatistics() {
        var stats = meetingService.getVerificationStatistics();
        
        assertNotNull(stats);
        assertTrue((Boolean) stats.get("z3SolverInitialized"));
        assertNotNull(stats.get("totalEvents"));
    }
}

