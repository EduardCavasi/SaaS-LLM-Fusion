package org.example.scheduler.repository;

import org.example.scheduler.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByName(String name);

    List<Room> findByAvailableTrue();

    List<Room> findByCapacityGreaterThanEqual(Integer minCapacity);

    @Query("SELECT r FROM Room r WHERE r.available = true AND r.capacity >= :minCapacity")
    List<Room> findAvailableRoomsWithMinCapacity(@Param("minCapacity") Integer minCapacity);

    boolean existsByName(String name);
}

