package com.example.racetransport.user.repository;

import com.example.racetransport.user.entity.CustomerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByUserId(Long userId);
}
