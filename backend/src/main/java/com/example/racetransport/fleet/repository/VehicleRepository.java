package com.example.racetransport.fleet.repository;

import com.example.racetransport.fleet.entity.Vehicle;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.entity.VehicleType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @Query("""
            select v from Vehicle v
            where v.archived = false
              and v.status in :statuses and v.vehicleType in :types
              and (:q is null or lower(v.vehicleCode) like lower(concat('%', :q, '%'))
                   or lower(v.plateNumber) like lower(concat('%', :q, '%'))
                   or lower(v.model) like lower(concat('%', :q, '%')))
            order by v.vehicleCode
            """)
    List<Vehicle> search(@Param("statuses") Collection<VehicleStatus> statuses,
                         @Param("types") Collection<VehicleType> types, @Param("q") String q);

    boolean existsByVehicleCodeIgnoreCase(String code);

    boolean existsByPlateNumberIgnoreCase(String plate);

    boolean existsByVehicleCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByPlateNumberIgnoreCaseAndIdNot(String plate, Long id);

    long countByStatusAndArchivedFalse(VehicleStatus status);

    long countByArchivedFalse();
}
