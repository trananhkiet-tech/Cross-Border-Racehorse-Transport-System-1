package com.example.racetransport.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.racetransport.horse.entity.StallType;
import com.example.racetransport.horse.repository.StallTypeRepository;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Tiện ích chung cho integration test (MockMvc + H2). */
abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected AppUserRepository users;
    @Autowired
    protected PasswordEncoder encoder;
    @Autowired
    protected StallTypeRepository stallTypes;

    protected static final String PASSWORD = "Matkhau123";

    /** Tạo tài khoản nhân viên trực tiếp (nhân viên không đăng ký qua API công khai). */
    protected void staff(String email, Role role) {
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setFullName(role.name());
        u.setRole(role);
        u.setEnabled(true);
        u.setPasswordHash(encoder.encode(PASSWORD));
        users.save(u);
    }

    protected Long stallId() {
        return stallTypes.findByCode("STANDARD").orElseGet(() -> {
            StallType s = new StallType();
            s.setCode("STANDARD");
            s.setName("Chuồng tiêu chuẩn");
            s.setWidthCm(75);
            s.setSurcharge(BigDecimal.ZERO);
            s.setActive(true);
            return stallTypes.save(s);
        }).getId();
    }

    protected String register(String email) throws Exception {
        String body = """
                {"email":"%s","password":"%s","fullName":"Khach Hang","phone":"0900000000","acceptTerms":true}
                """.formatted(email, PASSWORD);
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.accessToken");
    }

    protected String login(String email) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, PASSWORD);
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.accessToken");
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
