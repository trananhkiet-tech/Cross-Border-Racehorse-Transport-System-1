package com.example.racetransport.security;

import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Người dùng đã xác thực. Role lấy từ database (qua JWT do server ký), không tin role từ frontend.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;

    public UserPrincipal(Long id, String email, String fullName, String passwordHash, Role role, boolean enabled) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    public static UserPrincipal from(AppUser user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getFullName(), user.getPasswordHash(),
                user.getRole(), Boolean.TRUE.equals(user.getEnabled()));
    }

    public Long getId() { return id; }

    public String getEmail() { return email; }

    public String getFullName() { return fullName; }

    public Role getRole() { return role; }

    public boolean hasRole(Role r) { return role == r; }

    public boolean isCustomer() { return role == Role.CUSTOMER; }

    public boolean isManager() { return role == Role.LOGISTICS_MANAGER; }

    public boolean isSpecialist() { return role == Role.TRANSPORT_SPECIALIST; }

    public boolean isCoordinator() { return role == Role.FLEET_ROUTE_COORDINATOR; }

    public boolean isDriverEscort() { return role == Role.DRIVER_ESCORT; }

    /** Nhân viên nội bộ (không phải khách hàng). */
    public boolean isStaff() { return role != Role.CUSTOMER; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isEnabled() { return enabled; }
}
