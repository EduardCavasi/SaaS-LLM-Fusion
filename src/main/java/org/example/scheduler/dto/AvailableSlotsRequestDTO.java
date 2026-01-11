package org.example.scheduler.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableSlotsRequestDTO {
    
    @NotNull(message = "Room ID is required")
    private Long roomId;
    
    @NotNull(message = "Duration in minutes is required")
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;
    
    @NotNull(message = "Search start time is required")
    private LocalDateTime searchStart;
    
    @NotNull(message = "Search end time is required")
    private LocalDateTime searchEnd;
}

