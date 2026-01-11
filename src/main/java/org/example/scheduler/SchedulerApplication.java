package org.example.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Verified Meeting Scheduler Application
 * 
 * This application combines formal verification techniques with practical backend engineering:
 * - Z3 SMT Solver for static constraint validation
 * - Runtime Verification for dynamic monitoring of system behavior
 */
@SpringBootApplication
@EnableAspectJAutoProxy
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}

