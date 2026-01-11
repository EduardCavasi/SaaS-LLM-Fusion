package org.example.scheduler.verification.z3;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of Z3 constraint solving operation.
 */
public record ConstraintResult(
    boolean satisfiable,
    List<String> violations,
    long solvingTimeMs,
    String solverStatus
) {
    /**
     * Creates a successful result.
     */
    public static ConstraintResult success(long solvingTimeMs) {
        return new ConstraintResult(true, new ArrayList<>(), solvingTimeMs, "SATISFIABLE");
    }

    /**
     * Creates a failure result with violations.
     */
    public static ConstraintResult failure(List<String> violations, long solvingTimeMs) {
        return new ConstraintResult(false, violations, solvingTimeMs, "UNSATISFIABLE");
    }

    /**
     * Creates an error result.
     */
    public static ConstraintResult error(String errorMessage, long solvingTimeMs) {
        List<String> errors = new ArrayList<>();
        errors.add(errorMessage);
        return new ConstraintResult(false, errors, solvingTimeMs, "ERROR");
    }
}

