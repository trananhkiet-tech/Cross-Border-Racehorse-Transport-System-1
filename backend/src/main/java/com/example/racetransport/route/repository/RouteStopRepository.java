package com.example.racetransport.route.repository;

import com.example.racetransport.route.entity.RouteStop;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    List<RouteStop> findByRouteIdOrderByStopOrderAsc(Long routeId);

    @Query("select s from RouteStop s where s.route.id in :routeIds order by s.route.id, s.stopOrder")
    List<RouteStop> findByRouteIdIn(@Param("routeIds") Collection<Long> routeIds);

    long countByRouteId(Long routeId);

    boolean existsByRouteIdAndStopOrder(Long routeId, Integer stopOrder);
}
