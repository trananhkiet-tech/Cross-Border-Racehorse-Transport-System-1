package com.example.racetransport.security;

import com.example.racetransport.user.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Tải người dùng theo email cho AuthenticationManager. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmailIgnoreCase(email.trim())
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("not found"));
    }
}
