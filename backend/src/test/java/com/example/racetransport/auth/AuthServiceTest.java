package com.example.racetransport.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.racetransport.auth.dto.AuthDtos.AuthResponse;
import com.example.racetransport.auth.dto.AuthDtos.RegisterRequest;
import com.example.racetransport.auth.service.AuthService;
import com.example.racetransport.common.exception.ConflictException;
import com.example.racetransport.security.JwtService;
import com.example.racetransport.support.TestData;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.CustomerProfile;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import com.example.racetransport.user.repository.CustomerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Đăng ký công khai chỉ tạo CUSTOMER; mật khẩu được hash. */
class AuthServiceTest {

    private AppUserRepository users;
    private CustomerProfileRepository customers;
    private PasswordEncoder encoder;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        customers = mock(CustomerProfileRepository.class);
        encoder = new BCryptPasswordEncoder();
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[] {"test"});
        JwtService jwt = new JwtService(TestData.props("uploads"), env);
        when(users.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(100L);
            return u;
        });
        auth = new AuthService(users, customers, encoder, mock(AuthenticationManager.class), jwt, TestData.clock());
    }

    private static RegisterRequest req(String email) {
        return new RegisterRequest(email, "Matkhau123", "Nguyen Van A", "0901234567", null, null, "VN", true);
    }

    @Test
    void registerCreatesCustomerWithHashedPassword() {
        AuthResponse res = auth.register(req("New.User@Example.com"));
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals(Role.CUSTOMER, saved.getRole());
        assertEquals("new.user@example.com", saved.getEmail());
        assertNotEquals("Matkhau123", saved.getPasswordHash());
        assertTrue(encoder.matches("Matkhau123", saved.getPasswordHash()));
        assertEquals(Role.CUSTOMER, res.user().role());
        assertTrue(res.accessToken() != null && !res.accessToken().isBlank());
        verify(customers).save(any(CustomerProfile.class));
    }

    @Test
    void duplicateEmailIsRejected() {
        when(users.existsByEmailIgnoreCase("ha@irtms.vn")).thenReturn(true);
        assertThrows(ConflictException.class, () -> auth.register(req("ha@irtms.vn")));
        verify(users, never()).save(any(AppUser.class));
    }
}
