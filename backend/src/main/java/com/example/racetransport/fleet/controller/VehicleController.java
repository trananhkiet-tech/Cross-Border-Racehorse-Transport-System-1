package com.example.racetransport.fleet.controller;

import com.example.racetransport.fleet.dto.VehicleDtos.VehicleRequest;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleResponse;
import com.example.racetransport.fleet.dto.VehicleDtos.VehicleStatusRequest;
import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.entity.VehicleType;
import com.example.racetransport.fleet.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller quản lý đội xe vận chuyển ngựa đua quốc tế.
 *
 * <p>Chỉ vai trò {@code FLEET_ROUTE_COORDINATOR} được phép tạo, sửa, đổi trạng thái và lưu trữ xe.
 * Vai trò {@code LOGISTICS_MANAGER} có quyền xem danh sách và truy vấn xe sẵn sàng.</p>
 *
 * <p>Base URL: {@code /api/vehicles}</p>
 *
 * @author dangbao-hub
 * @see VehicleService
 */
@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Fleet")
public class VehicleController {

    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    /**
     * Lấy danh sách tất cả xe, hỗ trợ lọc theo trạng thái, loại xe và từ khóa tìm kiếm.
     *
     * @param status   danh sách trạng thái cần lọc (null = tất cả)
     * @param type     danh sách loại xe cần lọc (null = tất cả)
     * @param q        từ khóa tìm kiếm theo biển số / mã xe (null = không lọc)
     * @return danh sách xe khớp điều kiện
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    public List<VehicleResponse> list(@RequestParam(required = false) List<VehicleStatus> status,
                                      @RequestParam(required = false) List<VehicleType> type,
                                      @RequestParam(required = false) String q) {
        return service.list(status, type, q);
    }

    /**
     * Lấy danh sách xe còn trống trong khoảng thời gian chỉ định và đủ sức chứa tối thiểu.
     * Dùng khi lập kế hoạch phân công xe cho chuyến vận chuyển mới.
     *
     * @param from          thời điểm bắt đầu chuyến (ISO 8601)
     * @param to            thời điểm kết thúc chuyến (ISO 8601)
     * @param minCapacity   số ngựa tối thiểu xe phải chứa được (mặc định 1)
     * @param excludeTripId id chuyến cần bỏ qua khi kiểm tra xung đột lịch (null = không bỏ qua)
     * @return danh sách xe đủ điều kiện
     */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    @Operation(summary = "Xe sẵn sàng, không trùng lịch, đủ sức chứa")
    public List<VehicleResponse> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") int minCapacity,
            @RequestParam(required = false) Long excludeTripId) {
        return service.available(from, to, minCapacity, excludeTripId);
    }

    /**
     * Lấy chi tiết một xe theo id.
     *
     * @param id id của xe
     * @return thông tin chi tiết xe
     * @throws com.example.racetransport.common.exception.NotFoundException nếu không tìm thấy xe
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    public VehicleResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    /**
     * Thêm xe mới vào đội xe. Mã xe được sinh tự động.
     *
     * @param req thông tin xe cần tạo
     * @return xe vừa tạo kèm mã xe được sinh
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public VehicleResponse create(@Valid @RequestBody VehicleRequest req) {
        return service.create(req);
    }

    /**
     * Cập nhật thông tin xe (biển số, loại, sức chứa, trang bị).
     * Không được cập nhật khi xe đang thực hiện chuyến.
     *
     * @param id  id xe cần cập nhật
     * @param req thông tin mới
     * @return xe sau khi cập nhật
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest req) {
        return service.update(id, req);
    }

    /**
     * Thay đổi trạng thái vận hành của xe (AVAILABLE, IN_USE, MAINTENANCE, RETIRED).
     *
     * @param id  id xe
     * @param req trạng thái mới và ghi chú lý do
     * @return xe sau khi đổi trạng thái
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public VehicleResponse status(@PathVariable Long id, @Valid @RequestBody VehicleStatusRequest req) {
        return service.changeStatus(id, req.status(), req.note());
    }

    /**
     * Lưu trữ xe (soft delete) – xe bị ẩn khỏi danh sách hoạt động nhưng lịch sử chuyến vẫn được giữ lại.
     * Chỉ thực hiện được khi xe không còn chuyến đang chạy.
     *
     * @param id id xe cần lưu trữ
     * @return xe sau khi lưu trữ
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public VehicleResponse archive(@PathVariable Long id) {
        return service.archive(id);
    }
}
