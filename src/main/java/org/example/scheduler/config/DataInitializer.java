package org.example.scheduler.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduler.model.Participant;
import org.example.scheduler.model.Room;
import org.example.scheduler.repository.ParticipantRepository;
import org.example.scheduler.repository.RoomRepository;
import org.example.scheduler.verification.runtime.MeetingMonitor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes mock data for testing.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final MeetingMonitor meetingMonitor;

    @Override
    public void run(String... args) {
        log.info("Initializing sample data...");
        
        initializeRooms();
        initializeParticipants();
        
        log.info("Sample data initialization complete!");
    }

    private void initializeRooms() {
        if (roomRepository.count() > 0) {
            log.info("Rooms already exist, skipping initialization");
            return;
        }

        List<Room> rooms = List.of(
            Room.builder()
                .name("Conference Room A")
                .capacity(10)
                .location("Building 1, Floor 2")
                .description("Large conference room with projector and whiteboard")
                .available(true)
                .build(),
            Room.builder()
                .name("Meeting Room B")
                .capacity(6)
                .location("Building 1, Floor 1")
                .description("Medium-sized meeting room with video conferencing")
                .available(true)
                .build(),
            Room.builder()
                .name("Huddle Space C")
                .capacity(4)
                .location("Building 2, Floor 1")
                .description("Small huddle room for quick meetings")
                .available(true)
                .build(),
            Room.builder()
                .name("Board Room")
                .capacity(20)
                .location("Building 1, Floor 3")
                .description("Executive board room with premium AV equipment")
                .available(true)
                .build(),
            Room.builder()
                .name("Training Room")
                .capacity(30)
                .location("Building 2, Floor 2")
                .description("Large training room with classroom setup")
                .available(true)
                .build()
        );

        roomRepository.saveAll(rooms);
        
        // Register rooms with runtime monitor
        rooms.forEach(room -> {
            Room savedRoom = roomRepository.findByName(room.getName()).orElse(room);
            meetingMonitor.registerRoom(savedRoom.getId(), savedRoom.getCapacity());
        });
        
        log.info("Created {} sample rooms", rooms.size());
    }

    private void initializeParticipants() {
        if (participantRepository.count() > 0) {
            log.info("Participants already exist, skipping initialization");
            return;
        }

        List<Participant> participants = List.of(
            Participant.builder()
                .name("Alice Johnson")
                .email("alice.johnson@company.com")
                .department("Engineering")
                .build(),
            Participant.builder()
                .name("Bob Smith")
                .email("bob.smith@company.com")
                .department("Engineering")
                .build(),
            Participant.builder()
                .name("Carol Williams")
                .email("carol.williams@company.com")
                .department("Product")
                .build(),
            Participant.builder()
                .name("David Brown")
                .email("david.brown@company.com")
                .department("Design")
                .build(),
            Participant.builder()
                .name("Eva Martinez")
                .email("eva.martinez@company.com")
                .department("Marketing")
                .build(),
            Participant.builder()
                .name("Frank Lee")
                .email("frank.lee@company.com")
                .department("Engineering")
                .build(),
            Participant.builder()
                .name("Grace Chen")
                .email("grace.chen@company.com")
                .department("HR")
                .build(),
            Participant.builder()
                .name("Henry Wilson")
                .email("henry.wilson@company.com")
                .department("Finance")
                .build()
        );

        participantRepository.saveAll(participants);
        log.info("Created {} sample participants", participants.size());
    }
}

