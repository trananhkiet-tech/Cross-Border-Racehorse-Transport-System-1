package com.example.racetransport.user.repository;

import com.example.racetransport.user.entity.EmployeeProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {

    Optional<EmployeeProfile> findByUserId(Long userId);

    List<EmployeeProfile> findByUserIdIn(List<Long> userIds);

    boolean existsByEmployeeCode(String employeeCode);
}
