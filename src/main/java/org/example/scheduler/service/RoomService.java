package org.example.scheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduler.dto.RoomDTO;
import org.example.scheduler.exception.ResourceNotFoundException;
import org.example.scheduler.model.Room;
import org.example.scheduler.repository.RoomRepository;
import org.example.scheduler.verification.runtime.MeetingMonitor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing meeting rooms.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final MeetingMonitor meetingMonitor;

    /**
     * Creates a new room.
     */
    @Transactional
    public RoomDTO createRoom(RoomDTO roomDTO) {
        log.info("Creating room: {}", roomDTO.getName());
        
        if (roomRepository.existsByName(roomDTO.getName())) {
            throw new IllegalArgumentException("Room with name '" + roomDTO.getName() + "' already exists");
        }
        
        Room room = Room.builder()
                .name(roomDTO.getName())
                .capacity(roomDTO.getCapacity())
                .location(roomDTO.getLocation())
                .description(roomDTO.getDescription())
                .available(roomDTO.getAvailable() != null ? roomDTO.getAvailable() : true)
                .build();
        
        room = roomRepository.save(room);
        
        meetingMonitor.registerRoom(room.getId(), room.getCapacity());
        meetingMonitor.checkPendingMeetings();
        
        log.info("Created room with ID: {}", room.getId());
        return toDTO(room);
    }

    /**
     * Gets a room by ID.
     */
    @Transactional(readOnly = true)
    public RoomDTO getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
        return toDTO(room);
    }

    /**
     * Gets a room entity by ID (for internal use).
     */
    @Transactional(readOnly = true)
    public Room getRoomEntityById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }

    /**
     * Gets all rooms.
     */
    @Transactional(readOnly = true)
    public List<RoomDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets available rooms.
     */
    @Transactional(readOnly = true)
    public List<RoomDTO> getAvailableRooms() {
        return roomRepository.findByAvailableTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets rooms with minimum capacity.
     */
    @Transactional(readOnly = true)
    public List<RoomDTO> getRoomsWithMinCapacity(int minCapacity) {
        return roomRepository.findAvailableRoomsWithMinCapacity(minCapacity).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates a room.
     */
    @Transactional
    public RoomDTO updateRoom(Long id, RoomDTO roomDTO) {
        log.info("Updating room ID: {}", id);
        
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
        
        if (roomDTO.getName() != null && !roomDTO.getName().equals(room.getName())) {
            if (roomRepository.existsByName(roomDTO.getName())) {
                throw new IllegalArgumentException("Room with name '" + roomDTO.getName() + "' already exists");
            }
            room.setName(roomDTO.getName());
        }
        
        if (roomDTO.getCapacity() != null) {
            room.setCapacity(roomDTO.getCapacity());
            // Update monitor with new capacity
            meetingMonitor.registerRoom(room.getId(), roomDTO.getCapacity());
        }
        
        if (roomDTO.getLocation() != null) {
            room.setLocation(roomDTO.getLocation());
        }
        
        if (roomDTO.getDescription() != null) {
            room.setDescription(roomDTO.getDescription());
        }
        
        if (roomDTO.getAvailable() != null) {
            room.setAvailable(roomDTO.getAvailable());
        }
        
        room = roomRepository.save(room);
        log.info("Updated room ID: {}", id);
        
        return toDTO(room);
    }

    /**
     * Deletes a room.
     */
    @Transactional
    public void deleteRoom(Long id) {
        log.info("Deleting room ID: {}", id);
        
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room", id);
        }
        
        roomRepository.deleteById(id);
        meetingMonitor.checkPendingMeetings();
        log.info("Deleted room ID: {}", id);
    }

    /**
     * Converts Room entity to DTO.
     */
    private RoomDTO toDTO(Room room) {
        return RoomDTO.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .location(room.getLocation())
                .description(room.getDescription())
                .available(room.getAvailable())
                .build();
    }
}

