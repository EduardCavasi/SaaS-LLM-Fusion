package org.example.scheduler.repository;

import org.example.scheduler.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByEmail(String email);

    List<Participant> findByDepartment(String department);

    List<Participant> findByIdIn(Set<Long> ids);

    boolean existsByEmail(String email);

    @Query("SELECT p FROM Participant p JOIN p.meetings m " +
           "WHERE p.id IN :participantIds " +
           "AND m.status IN ('PENDING', 'CONFIRMED') " +
           "AND ((m.startTime < :endTime AND m.endTime > :startTime))")
    List<Participant> findBusyParticipants(
        @Param("participantIds") Set<Long> participantIds,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}

