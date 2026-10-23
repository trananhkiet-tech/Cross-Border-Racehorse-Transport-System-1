package com.example.racetransport.horse.repository;

import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.entity.HorseStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HorseRepository extends JpaRepository<Horse, Long> {

    @EntityGraph(attributePaths = "owner")
    @Query("""
            select h from Horse h
            where (:ownerId is null or h.owner.id = :ownerId)
              and (:status is null or h.status = :status)
              and (:q is null or lower(h.registeredName) like lower(concat('%', :q, '%'))
                   or lower(h.microchipNumber) like lower(concat('%', :q, '%'))
                   or lower(h.passportNumber) like lower(concat('%', :q, '%')))
            """)
    Page<Horse> search(@Param("ownerId") Long ownerId, @Param("status") HorseStatus status,
                       @Param("q") String q, Pageable pageable);

    List<Horse> findByOwnerIdAndStatusNot(Long ownerId, HorseStatus status);

    boolean existsByMicrochipNumberIgnoreCase(String microchip);

    boolean existsByPassportNumberIgnoreCase(String passport);

    boolean existsByMicrochipNumberIgnoreCaseAndIdNot(String microchip, Long id);

    boolean existsByPassportNumberIgnoreCaseAndIdNot(String passport, Long id);

    long countByOwnerIdAndStatusNot(Long ownerId, HorseStatus status);
}
