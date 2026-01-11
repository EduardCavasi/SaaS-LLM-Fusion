package org.example.scheduler.exception;

import java.util.ArrayList;
import java.util.List;

public class SchedulingException extends RuntimeException {
    
    private final List<String> violations;

    public SchedulingException(String message) {
        super(message);
        this.violations = new ArrayList<>();
    }

    public SchedulingException(String message, List<String> violations) {
        super(message);
        this.violations = violations != null ? violations : new ArrayList<>();
    }

    public List<String> getViolations() {
        return violations;
    }
}

