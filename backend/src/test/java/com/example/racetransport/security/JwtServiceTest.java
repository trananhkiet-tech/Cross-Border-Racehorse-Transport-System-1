package com.example.racetransport.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.racetransport.config.AppProperties;
import com.example.racetransport.support.TestData;
import com.example.racetransport.user.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class JwtServiceTest {

    private static Environment env(String... profiles) {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(profiles);
        return env;
    }

    @Test
    void tokenRoundTripReturnsUserId() {
        JwtService jwt = new JwtService(TestData.props("uploads"), env("dev"));
        String token = jwt.generateToken(TestData.principal(TestData.user(42, Role.CUSTOMER, "Tran Thu Ha")));
        assertEquals(42L, jwt.parseUserId(token).orElseThrow());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(TestData.props("uploads"), env("dev"));
        String token = jwt.generateToken(TestData.principal(TestData.user(42, Role.CUSTOMER, "A")));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("aa") ? "bb" : "aa");
        assertTrue(jwt.parseUserId(tampered).isEmpty());
        assertTrue(jwt.parseUserId("not-a-token").isEmpty());
    }

    @Test
    void tokenFromAnotherSecretIsRejected() {
        JwtService a = new JwtService(TestData.props("uploads"), env("dev"));
        AppProperties other = new AppProperties("Asia/Ho_Chi_Minh",
                new AppProperties.Jwt("another-secret-another-secret-0123456789", 60, "irtms"),
                null, null, null, null);
        JwtService b = new JwtService(other, env("dev"));
        String token = a.generateToken(TestData.principal(TestData.user(1, Role.CUSTOMER, "A")));
        assertTrue(b.parseUserId(token).isEmpty());
    }

    @Test
    void shortSecretIsRefused() {
        AppProperties weak = new AppProperties("Asia/Ho_Chi_Minh", new AppProperties.Jwt("short", 60, "irtms"),
                null, null, null, null);
        assertThrows(IllegalStateException.class, () -> new JwtService(weak, env("dev")));
    }

    @Test
    void prodRequiresSecret() {
        AppProperties none = new AppProperties("Asia/Ho_Chi_Minh", new AppProperties.Jwt("", 60, "irtms"),
                null, null, null, null);
        assertThrows(IllegalStateException.class, () -> new JwtService(none, env("prod")));
    }
}
