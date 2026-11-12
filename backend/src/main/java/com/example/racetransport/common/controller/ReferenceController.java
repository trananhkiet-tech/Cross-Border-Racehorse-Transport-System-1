package com.example.racetransport.common.controller;

import com.example.racetransport.common.util.Countries;
import com.example.racetransport.horse.dto.HorseDtos.StallTypeResponse;
import com.example.racetransport.horse.service.HorseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Danh mục tham chiếu công khai (không chứa dữ liệu nhạy cảm). */
@RestController
@RequestMapping("/api/reference")
@Tag(name = "Reference data")
public class ReferenceController {

    private final HorseService horseService;

    public ReferenceController(HorseService horseService) {
        this.horseService = horseService;
    }

    /** Danh mục quốc gia DEMO. */
    @GetMapping("/countries")
    public Map<String, String> countries() {
        return Countries.all();
    }

    /** Loại chuồng vận chuyển (tiêu chuẩn / rộng / hộp riêng) và phụ phí. */
    @GetMapping("/stall-types")
    public List<StallTypeResponse> stallTypes() {
        return horseService.stallTypes();
    }
}
