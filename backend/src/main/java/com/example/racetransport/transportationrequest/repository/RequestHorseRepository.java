package com.example.racetransport.transportationrequest.repository;

import com.example.racetransport.transportationrequest.entity.RequestHorse;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestHorseRepository extends JpaRepository<RequestHorse, Long> {

    @Query("select rh from RequestHorse rh join fetch rh.horse join fetch rh.stallType where rh.request.id = :requestId order by rh.id")
    List<RequestHorse> findByRequestIdFetch(@Param("requestId") Long requestId);

    @Query("select rh from RequestHorse rh join fetch rh.horse where rh.request.id in :ids")
    List<RequestHorse> findByRequestIdIn(@Param("ids") Collection<Long> ids);

    long countByRequestId(Long requestId);

    @Modifying
    @Query("delete from RequestHorse rh where rh.request.id = :requestId")
    void deleteByRequestId(@Param("requestId") Long requestId);

    @Query("""
            select case when count(rh) > 0 then true else false end from RequestHorse rh
            where rh.horse.id = :horseId
              and rh.request.status not in (
                com.example.racetransport.transportationrequest.entity.RequestStatus.COMPLETED,
                com.example.racetransport.transportationrequest.entity.RequestStatus.CANCELLED,
                com.example.racetransport.transportationrequest.entity.RequestStatus.REJECTED)
            """)
    boolean existsActiveRequestForHorse(@Param("horseId") Long horseId);

    boolean existsByRequestIdAndHorseId(Long requestId, Long horseId);
}
