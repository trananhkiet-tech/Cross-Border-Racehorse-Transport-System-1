package com.example.racetransport.trip.repository;

import com.example.racetransport.trip.entity.TripStaffAssignment;
import com.example.racetransport.trip.entity.TripStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripStaffAssignmentRepository extends JpaRepository<TripStaffAssignment, Long> {

    @Query("select a from TripStaffAssignment a join fetch a.staff where a.trip.id = :tripId order by a.assignmentRole, a.id")
    List<TripStaffAssignment> findByTripIdFetch(@Param("tripId") Long tripId);

    @Query("select a from TripStaffAssignment a join fetch a.staff where a.trip.id in :tripIds")
    List<TripStaffAssignment> findByTripIdIn(@Param("tripIds") Collection<Long> tripIds);

    boolean existsByTripIdAndStaffId(Long tripId, Long staffId);

    Optional<TripStaffAssignment> findByTripIdAndStaffId(Long tripId, Long staffId);

    boolean existsByTripRequestIdAndStaffId(Long requestId, Long staffId);

    long countByStaffIdAndTripStatusIn(Long staffId, Collection<TripStatus> statuses);

    /** Nhân viên đã được giao chuyến khác trùng khung giờ. */
    @Query("""
            select case when count(a) > 0 then true else false end from TripStaffAssignment a
            where a.staff.id = :staffId and a.trip.id <> :tripId
              and a.trip.status not in (com.example.racetransport.trip.entity.TripStatus.DELIVERED,
                                        com.example.racetransport.trip.entity.TripStatus.CANCELLED)
              and a.trip.plannedDeparture < :end and a.trip.plannedArrival > :start
            """)
    boolean existsStaffOverlap(@Param("staffId") Long staffId, @Param("tripId") Long tripId,
                               @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
