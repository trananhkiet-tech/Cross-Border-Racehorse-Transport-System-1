package com.example.racetransport.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.racetransport.compliance.entity.ComplianceCategory;
import com.example.racetransport.compliance.entity.ComplianceRequirement;
import com.example.racetransport.compliance.repository.ComplianceRequirementRepository;
import com.example.racetransport.document.entity.DocumentType;
import com.example.racetransport.user.entity.Role;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

/** Luồng tạo yêu cầu → gửi → sinh checklist + ước tính → tải tài liệu → Specialist rà soát. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestFlowIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ComplianceRequirementRepository requirements;

    private void requirement() {
        if (requirements.existsByCode("IT-PASSPORT")) {
            return;
        }
        ComplianceRequirement r = new ComplianceRequirement();
        r.setCode("IT-PASSPORT");
        r.setCategory(ComplianceCategory.IDENTIFICATION);
        r.setDocumentType(DocumentType.HORSE_PASSPORT);
        r.setTitle("Hộ chiếu ngựa");
        r.setPerHorse(true);
        r.setMandatory(true);
        r.setEffectiveFrom(LocalDate.of(2020, 1, 1));
        r.setActive(true);
        requirements.save(r);
    }

    @Test
    void createSubmitUploadAndReview() throws Exception {
        requirement();
        Long stall = stallId();
        String c = register("flow.customer@test.vn");
        String horseRes = mvc.perform(post("/api/horses").header("Authorization", bearer(c)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registeredName\":\"Golden Lotus\",\"microchipNumber\":\"985112008880001\",\"passportNumber\":\"VN-IT-1\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer horseId = JsonPath.read(horseRes, "$.id");
        String departure = LocalDate.now().plusDays(20).toString();
        String req = """
                {"originCountry":"VN","originAddress":"Củ Chi","destinationCountry":"SG","destinationAddress":"Kranji",
                 "requestedDepartureDate":"%s","movementType":"PERMANENT","purpose":"RELOCATION","transportMode":"ROAD_AIR",
                 "horses":[{"horseId":%d,"stallTypeId":%d,"declaredValue":120000}]}
                """.formatted(departure, horseId, stall);
        String created = mvc.perform(post("/api/transportation-requests").header("Authorization", bearer(c))
                        .contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.declaredValueTotal").value(120000.00))
                .andReturn().getResponse().getContentAsString();
        Integer requestId = JsonPath.read(created, "$.id");

        mvc.perform(post("/api/transportation-requests/" + requestId + "/submit").header("Authorization", bearer(c)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.estimate.status").value("ESTIMATED"));

        String checklist = mvc.perform(get("/api/compliance/checklists/" + requestId).header("Authorization", bearer(c)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer itemId = JsonPath.read(checklist, "$.items[0].id");

        MockMultipartFile pdf = new MockMultipartFile("file", "passport.pdf", "application/pdf",
                "%PDF-1.4 test".getBytes(StandardCharsets.US_ASCII));
        String doc = mvc.perform(multipart("/api/documents/upload").file(pdf).header("Authorization", bearer(c))
                        .param("documentType", "HORSE_PASSPORT").param("requestId", String.valueOf(requestId))
                        .param("horseId", String.valueOf(horseId)).param("checklistItemId", String.valueOf(itemId))
                        .param("expiryDate", LocalDate.now().plusYears(2).toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewStatus").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        Integer docId = JsonPath.read(doc, "$.id");

        // Khách hàng không tự duyệt tài liệu
        mvc.perform(post("/api/documents/" + docId + "/review").header("Authorization", bearer(c))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\"}")).andExpect(status().isForbidden());

        staff("flow.specialist@test.vn", Role.TRANSPORT_SPECIALIST);
        String s = login("flow.specialist@test.vn");
        mvc.perform(post("/api/documents/" + docId + "/review").header("Authorization", bearer(s))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"APPROVED\",\"note\":\"OK\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewStatus").value("APPROVED"));
        mvc.perform(get("/api/compliance/checklists/" + requestId).header("Authorization", bearer(s)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.complete").value(true));

        // Không sửa yêu cầu sau khi đã gửi
        mvc.perform(put("/api/transportation-requests/" + requestId).header("Authorization", bearer(c))
                .contentType(MediaType.APPLICATION_JSON).content(req)).andExpect(status().isConflict());
    }
}
