package com.example.racetransport.quotation.repository;

import com.example.racetransport.quotation.entity.PricingRate;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PricingRateRepository extends JpaRepository<PricingRate, Long> {

    /** Bảng giá áp dụng cho phương thức + nước đến (NULL = áp dụng chung). */
    @Query("""
            select p from PricingRate p
            where p.active = true
              and (p.transportMode is null or p.transportMode = :mode)
              and (p.destinationCountry is null or p.destinationCountry = :destination)
            order by p.category, p.code
            """)
    List<PricingRate> findApplicable(@Param("mode") TransportMode mode, @Param("destination") String destination);

    List<PricingRate> findAllByOrderByCategoryAscCodeAsc();
}
