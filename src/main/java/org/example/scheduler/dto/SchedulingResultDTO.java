package org.example.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulingResultDTO {
    
    private boolean success;

    private MeetingDTO meeting;

    @Builder.Default
    private List<String> constraintViolations = new ArrayList<>();

    @Builder.Default
    private List<String> runtimeWarnings = new ArrayList<>();

    private String solverStatus;

    private String explanation;

    private Long solvingTimeMs;

    public static SchedulingResultDTO success(MeetingDTO meeting, String explanation, Long solvingTimeMs) {
        return SchedulingResultDTO.builder()
                .success(true)
                .meeting(meeting)
                .solverStatus("SATISFIABLE")
                .explanation(explanation)
                .solvingTimeMs(solvingTimeMs)
                .build();
    }

    public static SchedulingResultDTO failure(List<String> violations, String explanation, Long solvingTimeMs) {
        return SchedulingResultDTO.builder()
                .success(false)
                .constraintViolations(violations)
                .solverStatus("UNSATISFIABLE")
                .explanation(explanation)
                .solvingTimeMs(solvingTimeMs)
                .build();
    }
}

