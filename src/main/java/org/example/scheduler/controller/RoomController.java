package org.example.scheduler.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.scheduler.dto.RoomDTO;
import org.example.scheduler.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing meeting rooms.
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * Creates a new room.
     */
    @PostMapping
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody RoomDTO roomDTO) {
        RoomDTO created = roomService.createRoom(roomDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets a room by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoomDTO> getRoomById(@PathVariable Long id) {
        RoomDTO room = roomService.getRoomById(id);
        return ResponseEntity.ok(room);
    }

    /**
     * Gets all rooms.
     */
    @GetMapping
    public ResponseEntity<List<RoomDTO>> getAllRooms() {
        List<RoomDTO> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }

    /**
     * Gets available rooms.
     */
    @GetMapping("/available")
    public ResponseEntity<List<RoomDTO>> getAvailableRooms() {
        List<RoomDTO> rooms = roomService.getAvailableRooms();
        return ResponseEntity.ok(rooms);
    }

    /**
     * Gets rooms with minimum capacity.
     */
    @GetMapping("/capacity/{minCapacity}")
    public ResponseEntity<List<RoomDTO>> getRoomsWithMinCapacity(@PathVariable int minCapacity) {
        List<RoomDTO> rooms = roomService.getRoomsWithMinCapacity(minCapacity);
        return ResponseEntity.ok(rooms);
    }

    /**
     * Updates a room.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoomDTO> updateRoom(@PathVariable Long id, @RequestBody RoomDTO roomDTO) {
        RoomDTO updated = roomService.updateRoom(id, roomDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a room.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}

