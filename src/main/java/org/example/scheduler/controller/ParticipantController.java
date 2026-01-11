package org.example.scheduler.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.scheduler.dto.ParticipantDTO;
import org.example.scheduler.service.ParticipantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing meeting participants.
 */
@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    /**
     * Creates a new participant.
     */
    @PostMapping
    public ResponseEntity<ParticipantDTO> createParticipant(@Valid @RequestBody ParticipantDTO participantDTO) {
        ParticipantDTO created = participantService.createParticipant(participantDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Gets a participant by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ParticipantDTO> getParticipantById(@PathVariable Long id) {
        ParticipantDTO participant = participantService.getParticipantById(id);
        return ResponseEntity.ok(participant);
    }

    /**
     * Gets all participants.
     */
    @GetMapping
    public ResponseEntity<List<ParticipantDTO>> getAllParticipants() {
        List<ParticipantDTO> participants = participantService.getAllParticipants();
        return ResponseEntity.ok(participants);
    }

    /**
     * Gets participants by department.
     */
    @GetMapping("/department/{department}")
    public ResponseEntity<List<ParticipantDTO>> getParticipantsByDepartment(@PathVariable String department) {
        List<ParticipantDTO> participants = participantService.getParticipantsByDepartment(department);
        return ResponseEntity.ok(participants);
    }

    /**
     * Updates a participant.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ParticipantDTO> updateParticipant(
            @PathVariable Long id, 
            @RequestBody ParticipantDTO participantDTO) {
        ParticipantDTO updated = participantService.updateParticipant(id, participantDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a participant.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParticipant(@PathVariable Long id) {
        participantService.deleteParticipant(id);
        return ResponseEntity.noContent().build();
    }
}

