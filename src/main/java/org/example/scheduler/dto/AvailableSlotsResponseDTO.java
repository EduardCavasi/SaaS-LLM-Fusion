package org.example.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableSlotsResponseDTO {
    private Long roomId;
    private Integer durationMinutes;
    private LocalDateTime searchStart;
    private LocalDateTime searchEnd;
    private List<LocalDateTime> availableSlots;
    private Integer totalSlots;
}

