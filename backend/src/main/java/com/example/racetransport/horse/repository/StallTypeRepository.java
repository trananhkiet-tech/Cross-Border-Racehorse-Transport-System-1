package com.example.racetransport.horse.repository;

import com.example.racetransport.horse.entity.StallType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StallTypeRepository extends JpaRepository<StallType, Long> {

    List<StallType> findByActiveTrueOrderBySortOrderAsc();

    Optional<StallType> findByCode(String code);
}
