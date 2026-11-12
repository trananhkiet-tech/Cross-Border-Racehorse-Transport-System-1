package com.example.racetransport.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.racetransport.user.entity.Role;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

/** Phân quyền ở backend: không chỉ ẩn menu ở frontend. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessControlIntegrationTest extends IntegrationTestSupport {

    private static final String HORSE = """
            {"registeredName":"Silver Arrow","breed":"Thoroughbred","microchipNumber":"%s","passportNumber":"%s"}
            """;

    @Test
    void customerCannotReadAnotherCustomersHorse() throws Exception {
        String a = register("owner.a@test.vn");
        String b = register("owner.b@test.vn");
        String res = mvc.perform(post("/api/horses").header("Authorization", bearer(a)).contentType(MediaType.APPLICATION_JSON)
                        .content(HORSE.formatted("985112009990001", "VN-HP-AC-1")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(res, "$.id");
        mvc.perform(get("/api/horses/" + id).header("Authorization", bearer(a))).andExpect(status().isOk());
        mvc.perform(get("/api/horses/" + id).header("Authorization", bearer(b))).andExpect(status().isForbidden());
    }

    @Test
    void customerCannotUseFleetOrStaffEndpoints() throws Exception {
        String c = register("fleet.c@test.vn");
        mvc.perform(get("/api/vehicles").header("Authorization", bearer(c))).andExpect(status().isForbidden());
        mvc.perform(get("/api/employees").header("Authorization", bearer(c))).andExpect(status().isForbidden());
        mvc.perform(get("/api/reports/kpi").header("Authorization", bearer(c))).andExpect(status().isForbidden());
    }

    @Test
    void specialistCannotApproveRequestsOrCreateVehicles() throws Exception {
        staff("spec.ac@test.vn", Role.TRANSPORT_SPECIALIST);
        String s = login("spec.ac@test.vn");
        mvc.perform(post("/api/transportation-requests/1/approve").header("Authorization", bearer(s))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/vehicles").header("Authorization", bearer(s)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehicleCode\":\"X\",\"plateNumber\":\"Y\",\"vehicleType\":\"HORSE_TRUCK\",\"capacityHorses\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinatorCanManageFleet() throws Exception {
        staff("coord.ac@test.vn", Role.FLEET_ROUTE_COORDINATOR);
        String c = login("coord.ac@test.vn");
        mvc.perform(post("/api/vehicles").header("Authorization", bearer(c)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleCode\":\"HT-99\",\"plateNumber\":\"51C-999.99\",\"vehicleType\":\"HORSE_TRUCK\",\"capacityHorses\":4}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/vehicles").header("Authorization", bearer(c))).andExpect(status().isOk());
    }

    @Test
    void driverCannotListRequests() throws Exception {
        staff("driver.ac@test.vn", Role.DRIVER_ESCORT);
        String d = login("driver.ac@test.vn");
        mvc.perform(get("/api/transportation-requests").header("Authorization", bearer(d))).andExpect(status().isForbidden());
        mvc.perform(get("/api/trips").header("Authorization", bearer(d))).andExpect(status().isOk());
    }
}
