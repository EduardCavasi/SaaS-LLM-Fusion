package org.example.scheduler.repository;

import org.example.scheduler.model.Meeting;
import org.example.scheduler.model.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByStatus(MeetingStatus status);

    List<Meeting> findByRoomId(Long roomId);

    @Query("SELECT m FROM Meeting m WHERE m.room.id = :roomId " +
           "AND m.status IN ('PENDING', 'CONFIRMED') " +
           "AND ((m.startTime < :endTime AND m.endTime > :startTime))")
    List<Meeting> findOverlappingMeetingsInRoom(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT m FROM Meeting m WHERE m.room.id = :roomId " +
           "AND m.status IN ('PENDING', 'CONFIRMED') " +
           "AND m.id != :excludeMeetingId " +
           "AND ((m.startTime < :endTime AND m.endTime > :startTime))")
    List<Meeting> findOverlappingMeetingsInRoomExcluding(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("excludeMeetingId") Long excludeMeetingId
    );

    @Query("SELECT m FROM Meeting m JOIN m.participants p " +
           "WHERE p.id = :participantId " +
           "AND m.status IN ('PENDING', 'CONFIRMED') " +
           "AND ((m.startTime < :endTime AND m.endTime > :startTime))")
    List<Meeting> findOverlappingMeetingsForParticipant(
        @Param("participantId") Long participantId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT m FROM Meeting m WHERE m.startTime >= :start AND m.endTime <= :end " +
           "AND m.status IN ('PENDING', 'CONFIRMED')")
    List<Meeting> findMeetingsInTimeRange(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(m) FROM Meeting m WHERE m.room.id = :roomId " +
           "AND m.status IN ('PENDING', 'CONFIRMED')")
    long countActiveMeetingsInRoom(@Param("roomId") Long roomId);
}

