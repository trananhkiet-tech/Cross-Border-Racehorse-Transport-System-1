package com.example.racetransport.trip.repository;

import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransportationTripRepository extends JpaRepository<TransportationTrip, Long> {

    List<TransportationTrip> findByRequestIdOrderByPlannedDepartureAsc(Long requestId);

    @EntityGraph(attributePaths = {"request", "request.customer", "route", "vehicle", "coordinator"})
    @Query("""
            select t from TransportationTrip t
            where t.status in :statuses
              and (:customerId is null or t.request.customer.id = :customerId)
              and (:staffId is null or exists (select a.id from TripStaffAssignment a
                                               where a.trip = t and a.staff.id = :staffId))
              and (:q is null or lower(t.tripNumber) like lower(concat('%', :q, '%'))
                   or lower(t.request.requestNumber) like lower(concat('%', :q, '%'))
                   or lower(t.route.name) like lower(concat('%', :q, '%')))
            """)
    Page<TransportationTrip> search(@Param("statuses") Collection<TripStatus> statuses,
                                    @Param("customerId") Long customerId, @Param("staffId") Long staffId,
                                    @Param("q") String q, Pageable pageable);

    /** Xe đã bị giao cho chuyến khác trùng khung giờ (chuyến chưa kết thúc). */
    @Query("""
            select case when count(t) > 0 then true else false end from TransportationTrip t
            where t.vehicle.id = :vehicleId and t.id <> :tripId
              and t.status not in (com.example.racetransport.trip.entity.TripStatus.DELIVERED,
                                   com.example.racetransport.trip.entity.TripStatus.CANCELLED)
              and t.plannedDeparture < :end and t.plannedArrival > :start
            """)
    boolean existsVehicleOverlap(@Param("vehicleId") Long vehicleId, @Param("tripId") Long tripId,
                                 @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByStatusIn(Collection<TripStatus> statuses);

    long countByStatus(TripStatus status);

    long countByRequestCustomerIdAndStatusIn(Long customerId, Collection<TripStatus> statuses);

    long countByVehicleIdAndStatusIn(Long vehicleId, Collection<TripStatus> statuses);

    List<TransportationTrip> findByVehicleIdOrderByPlannedDepartureDesc(Long vehicleId);

    List<TransportationTrip> findByRouteIdOrderByPlannedDepartureDesc(Long routeId);

    @Query("""
            select t from TransportationTrip t join fetch t.request r join fetch t.route
            where t.status in :statuses and t.plannedDeparture >= :from and t.plannedDeparture < :to
            order by t.plannedDeparture asc
            """)
    List<TransportationTrip> findUpcoming(@Param("statuses") Collection<TripStatus> statuses,
                                          @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select t from TransportationTrip t
            where t.status in :statuses and (t.vehicle is null or not exists
              (select a.id from TripStaffAssignment a where a.trip = t and
               a.assignmentRole = com.example.racetransport.user.entity.StaffRole.DRIVER))
            order by t.plannedDeparture asc
            """)
    List<TransportationTrip> findNeedingAssignment(@Param("statuses") Collection<TripStatus> statuses);

    List<TransportationTrip> findByStatusIn(Collection<TripStatus> statuses);
}
