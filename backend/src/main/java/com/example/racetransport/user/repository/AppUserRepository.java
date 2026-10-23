package com.example.racetransport.user.repository;

import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AppUser> findByRoleAndEnabledTrue(Role role);

    List<AppUser> findByRoleOrderByFullNameAsc(Role role);

    long countByRole(Role role);
}
