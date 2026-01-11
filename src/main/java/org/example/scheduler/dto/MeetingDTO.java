package org.example.scheduler.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.scheduler.model.MeetingStatus;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingDTO {
    
    private Long id;

    @NotBlank(message = "Meeting title is required")
    private String title;

    private String description;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Room ID is required")
    private Long roomId;

    private String roomName;

    @NotEmpty(message = "At least one participant is required")
    private Set<Long> participantIds;

    private Set<ParticipantDTO> participants;

    private MeetingStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

