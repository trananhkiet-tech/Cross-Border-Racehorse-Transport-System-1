package com.example.racetransport.route.repository;

import com.example.racetransport.route.entity.Route;
import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteRepository extends JpaRepository<Route, Long> {

    @Query("""
            select r from Route r
            where r.status in :statuses
              and (:origin is null or r.originCountry = :origin)
              and (:destination is null or r.destinationCountry = :destination)
              and (:q is null or lower(r.name) like lower(concat('%', :q, '%'))
                   or lower(r.routeCode) like lower(concat('%', :q, '%')))
            order by r.routeCode
            """)
    List<Route> search(@Param("statuses") Collection<RouteStatus> statuses, @Param("origin") String origin,
                       @Param("destination") String destination, @Param("q") String q);

    Optional<Route> findFirstByOriginCountryAndDestinationCountryAndTransportModeAndStatusOrderByIdAsc(
            String origin, String destination, TransportMode mode, RouteStatus status);

    List<Route> findByAlternativeOfId(Long routeId);

    @Query("select count(a) > 0 from IncidentAction a where a.newRoute.id = :routeId")
    boolean usedByIncidentAction(@Param("routeId") Long routeId);

    boolean existsByRouteCode(String code);

    long countByStatus(RouteStatus status);
}
