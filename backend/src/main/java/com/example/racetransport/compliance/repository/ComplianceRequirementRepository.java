package com.example.racetransport.compliance.repository;

import com.example.racetransport.compliance.entity.ComplianceCategory;
import com.example.racetransport.compliance.entity.ComplianceRequirement;
import com.example.racetransport.transportationrequest.entity.MovementType;
import com.example.racetransport.transportationrequest.entity.TransportPurpose;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplianceRequirementRepository extends JpaRepository<ComplianceRequirement, Long> {

    /**
     * Yêu cầu áp dụng cho một tuyến/loại di chuyển/mục đích tại một ngày.
     * Cột NULL nghĩa là "áp dụng cho mọi giá trị".
     */
    @Query("""
            select c from ComplianceRequirement c
            where c.active = true
              and c.effectiveFrom <= :onDate and (c.effectiveTo is null or c.effectiveTo >= :onDate)
              and (c.originCountry is null or c.originCountry = :origin)
              and (c.destinationCountry is null or c.destinationCountry = :destination)
              and (c.movementType is null or c.movementType = :movementType)
              and (c.purpose is null or c.purpose = :purpose)
            order by c.category, c.code
            """)
    List<ComplianceRequirement> findApplicable(@Param("origin") String origin,
                                               @Param("destination") String destination,
                                               @Param("movementType") MovementType movementType,
                                               @Param("purpose") TransportPurpose purpose,
                                               @Param("onDate") LocalDate onDate);

    @Query("""
            select c from ComplianceRequirement c
            where c.category in :categories
              and (:destination is null or c.destinationCountry is null or c.destinationCountry = :destination)
              and (:includeInactive = true or c.active = true)
            order by c.destinationCountry, c.category, c.code
            """)
    List<ComplianceRequirement> search(@Param("categories") Collection<ComplianceCategory> categories,
                                       @Param("destination") String destination,
                                       @Param("includeInactive") boolean includeInactive);

    boolean existsByCode(String code);
}
