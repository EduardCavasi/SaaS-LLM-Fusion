package org.example.scheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.scheduler.dto.ParticipantDTO;
import org.example.scheduler.exception.ResourceNotFoundException;
import org.example.scheduler.model.Participant;
import org.example.scheduler.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantService {

    private final ParticipantRepository participantRepository;

    /**
     * Creates a new participant.
     */
    @Transactional
    public ParticipantDTO createParticipant(ParticipantDTO participantDTO) {
        log.info("Creating participant: {}", participantDTO.getEmail());
        
        if (participantRepository.existsByEmail(participantDTO.getEmail())) {
            throw new IllegalArgumentException("Participant with email '" + participantDTO.getEmail() + "' already exists");
        }
        
        Participant participant = Participant.builder()
                .name(participantDTO.getName())
                .email(participantDTO.getEmail())
                .department(participantDTO.getDepartment())
                .build();
        
        participant = participantRepository.save(participant);
        log.info("Created participant with ID: {}", participant.getId());
        
        return toDTO(participant);
    }

    /**
     * Gets a participant by ID.
     */
    @Transactional(readOnly = true)
    public ParticipantDTO getParticipantById(Long id) {
        Participant participant = participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant", id));
        return toDTO(participant);
    }

    /**
     * Gets participant entities by IDs.
     */
    @Transactional(readOnly = true)
    public Set<Participant> getParticipantEntitiesByIds(Set<Long> ids) {
        List<Participant> participants = participantRepository.findByIdIn(ids);
        
        if (participants.size() != ids.size()) {
            Set<Long> foundIds = participants.stream()
                    .map(Participant::getId)
                    .collect(Collectors.toSet());
            Set<Long> missingIds = ids.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new ResourceNotFoundException("Participants", missingIds);
        }
        
        return new java.util.HashSet<>(participants);
    }

    /**
     * Gets all participants.
     */
    @Transactional(readOnly = true)
    public List<ParticipantDTO> getAllParticipants() {
        return participantRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets participants by department.
     */
    @Transactional(readOnly = true)
    public List<ParticipantDTO> getParticipantsByDepartment(String department) {
        return participantRepository.findByDepartment(department).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets participants who are busy during a time range.
     */
    @Transactional(readOnly = true)
    public List<ParticipantDTO> getBusyParticipants(Set<Long> participantIds, 
            LocalDateTime startTime, LocalDateTime endTime) {
        return participantRepository.findBusyParticipants(participantIds, startTime, endTime).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates a participant.
     */
    @Transactional
    public ParticipantDTO updateParticipant(Long id, ParticipantDTO participantDTO) {
        log.info("Updating participant ID: {}", id);
        
        Participant participant = participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant", id));
        
        if (participantDTO.getName() != null) {
            participant.setName(participantDTO.getName());
        }
        
        if (participantDTO.getEmail() != null && !participantDTO.getEmail().equals(participant.getEmail())) {
            if (participantRepository.existsByEmail(participantDTO.getEmail())) {
                throw new IllegalArgumentException("Participant with email '" + participantDTO.getEmail() + "' already exists");
            }
            participant.setEmail(participantDTO.getEmail());
        }
        
        if (participantDTO.getDepartment() != null) {
            participant.setDepartment(participantDTO.getDepartment());
        }
        
        participant = participantRepository.save(participant);
        log.info("Updated participant ID: {}", id);
        
        return toDTO(participant);
    }

    /**
     * Deletes a participant.
     */
    @Transactional
    public void deleteParticipant(Long id) {
        log.info("Deleting participant ID: {}", id);
        
        if (!participantRepository.existsById(id)) {
            throw new ResourceNotFoundException("Participant", id);
        }
        
        participantRepository.deleteById(id);
        log.info("Deleted participant ID: {}", id);
    }

    /**
     * Converts Participant entity to DTO.
     */
    private ParticipantDTO toDTO(Participant participant) {
        return ParticipantDTO.builder()
                .id(participant.getId())
                .name(participant.getName())
                .email(participant.getEmail())
                .department(participant.getDepartment())
                .build();
    }
}

