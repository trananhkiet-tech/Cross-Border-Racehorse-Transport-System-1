package com.example.racetransport.support;

import com.example.racetransport.config.AppProperties;
import com.example.racetransport.horse.entity.Horse;
import com.example.racetransport.horse.entity.HorseStatus;
import com.example.racetransport.horse.entity.StallType;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.entity.MovementType;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.TransportMode;
import com.example.racetransport.transportationrequest.entity.TransportPurpose;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** Dữ liệu dựng sẵn cho unit test (không phụ thuộc database). */
public final class TestData {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** "Hôm nay" cố định của test: 2026-10-01 09:00 giờ Việt Nam. */
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 9, 0);

    private TestData() { }

    public static Clock clock() {
        return Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
    }

    public static Clock clockAt(LocalDateTime t) {
        Instant i = t.atZone(ZONE).toInstant();
        return Clock.fixed(i, ZONE);
    }

    public static AppUser user(long id, Role role, String name) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setEmail(name.toLowerCase().replace(' ', '.') + "@test.vn");
        u.setFullName(name);
        u.setRole(role);
        u.setEnabled(true);
        u.setPasswordHash("hash");
        return u;
    }

    public static UserPrincipal principal(AppUser u) {
        return UserPrincipal.from(u);
    }

    public static Horse horse(long id, AppUser owner, String name) {
        Horse h = new Horse();
        h.setId(id);
        h.setOwner(owner);
        h.setRegisteredName(name);
        h.setMicrochipNumber("98511200" + id);
        h.setPassportNumber("VN-HP-" + id);
        h.setStatus(HorseStatus.ACTIVE);
        return h;
    }

    public static StallType stall(long id, String code, String surcharge) {
        StallType s = new StallType();
        s.setId(id);
        s.setCode(code);
        s.setName(code);
        s.setWidthCm(75);
        s.setSurcharge(new BigDecimal(surcharge));
        s.setActive(true);
        return s;
    }

    public static TransportationRequest request(long id, AppUser customer, RequestStatus status) {
        TransportationRequest r = new TransportationRequest();
        r.setId(id);
        r.setRequestNumber("REQ-2026-" + String.format("%04d", id));
        r.setCustomer(customer);
        r.setOriginCountry("VN");
        r.setOriginAddress("TP.HCM");
        r.setDestinationCountry("SG");
        r.setDestinationAddress("Kranji");
        r.setRequestedDepartureDate(LocalDate.of(2026, 10, 20));
        r.setMovementType(MovementType.TEMPORARY);
        r.setPurpose(TransportPurpose.RACING);
        r.setTransportMode(TransportMode.ROAD_AIR);
        r.setCurrency("USD");
        r.setDeclaredValueTotal(new BigDecimal("180000.00"));
        r.setStatus(status);
        return r;
    }

    public static AppProperties props(String uploadDir) {
        return new AppProperties("Asia/Ho_Chi_Minh",
                new AppProperties.Jwt("unit-test-secret-unit-test-secret-0123456789", 60, "irtms"),
                new AppProperties.Cors(List.of("http://localhost:5173")),
                new AppProperties.Storage(uploadDir, 10 * 1024 * 1024, List.of("application/pdf", "image/jpeg", "image/png", "image/webp")),
                new AppProperties.Business(new BigDecimal("50"), 14, 7),
                new AppProperties.Seed(false));
    }

    /** So sánh tiền theo giá trị (bỏ qua scale). */
    public static boolean same(String expected, BigDecimal actual) {
        return actual != null && new BigDecimal(expected).compareTo(actual) == 0;
    }
}
