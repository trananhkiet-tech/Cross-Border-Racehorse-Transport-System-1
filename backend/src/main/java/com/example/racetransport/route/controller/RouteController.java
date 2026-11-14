package com.example.racetransport.route.controller;

import com.example.racetransport.route.dto.RouteDtos.RouteDetail;
import com.example.racetransport.route.dto.RouteDtos.RouteRequest;
import com.example.racetransport.route.dto.RouteDtos.RouteStopRequest;
import com.example.racetransport.route.dto.RouteDtos.RouteSummary;
import com.example.racetransport.route.entity.RouteStatus;
import com.example.racetransport.route.service.RouteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller quản lý tuyến đường vận chuyển ngựa đua quốc tế.
 *
 * <p>Tuyến đường được dùng lại cho nhiều chuyến vận chuyển khác nhau.
 * Mỗi tuyến bao gồm điểm xuất phát, điểm đến và danh sách điểm dừng (trạm nghỉ, cửa khẩu,
 * sân bay, kiểm dịch, hải quan).</p>
 *
 * <p>Chỉ vai trò {@code FLEET_ROUTE_COORDINATOR} được phép tạo, cập nhật, thêm/sửa/xóa điểm dừng
 * và xóa tuyến. Vai trò {@code LOGISTICS_MANAGER} có quyền xem.</p>
 *
 * <p>Base URL: {@code /api/routes}</p>
 *
 * @author dangbao-hub
 * @see RouteService
 */
@RestController
@RequestMapping("/api/routes")
@Tag(name = "Routes")
public class RouteController {

    private final RouteService service;

    public RouteController(RouteService service) {
        this.service = service;
    }

    /**
     * Lấy danh sách tuyến đường với bộ lọc linh hoạt.
     *
     * @param status      danh sách trạng thái cần lọc (null = tất cả)
     * @param origin      mã quốc gia xuất phát (ISO 3166-1 alpha-2, null = không lọc)
     * @param destination mã quốc gia đến (null = không lọc)
     * @param q           từ khóa tìm kiếm theo tên hoặc mã tuyến (null = không lọc)
     * @return danh sách tuyến tóm tắt
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    public List<RouteSummary> list(@RequestParam(required = false) List<RouteStatus> status,
                                   @RequestParam(required = false) String origin,
                                   @RequestParam(required = false) String destination,
                                   @RequestParam(required = false) String q) {
        return service.list(status, origin, destination, q);
    }

    /**
     * Lấy chi tiết tuyến đường bao gồm toàn bộ danh sách điểm dừng theo thứ tự.
     *
     * @param id id tuyến đường
     * @return chi tiết tuyến đường kèm điểm dừng
     * @throws com.example.racetransport.common.exception.NotFoundException nếu không tìm thấy tuyến
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FLEET_ROUTE_COORDINATOR','LOGISTICS_MANAGER')")
    public RouteDetail get(@PathVariable Long id) {
        return service.get(id);
    }

    /**
     * Tạo tuyến đường mới ở trạng thái DRAFT. Cần thêm điểm dừng và kích hoạt trước khi lên lịch chuyến.
     *
     * @param req thông tin tuyến đường cần tạo
     * @return tuyến đường vừa tạo kèm mã tuyến được sinh tự động
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public RouteDetail create(@Valid @RequestBody RouteRequest req) {
        return service.create(req);
    }

    /**
     * Cập nhật thông tin tuyến đường (tên, khoảng cách, thời gian ước tính, ghi chú).
     * Không cập nhật được khi tuyến đang có chuyến IN_TRANSIT.
     *
     * @param id  id tuyến cần cập nhật
     * @param req thông tin mới
     * @return tuyến sau khi cập nhật
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public RouteDetail update(@PathVariable Long id, @Valid @RequestBody RouteRequest req) {
        return service.update(id, req);
    }

    /**
     * Thêm điểm dừng mới vào tuyến (trạm nghỉ, cửa khẩu, sân bay, kiểm dịch, hải quan...).
     * Không thêm được khi tuyến đang có chuyến IN_TRANSIT.
     *
     * @param id  id tuyến
     * @param req thông tin điểm dừng mới
     * @return tuyến sau khi thêm điểm dừng
     */
    @PostMapping("/{id}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public RouteDetail addStop(@PathVariable Long id, @Valid @RequestBody RouteStopRequest req) {
        return service.addStop(id, req);
    }

    /**
     * Cập nhật thông tin một điểm dừng (tên địa điểm, loại dừng, thời gian chờ ước tính).
     *
     * @param routeId id tuyến đường
     * @param stopId  id điểm dừng cần cập nhật
     * @param req     thông tin mới
     * @return tuyến sau khi cập nhật điểm dừng
     */
    @PutMapping("/{routeId}/stops/{stopId}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public RouteDetail updateStop(@PathVariable Long routeId, @PathVariable Long stopId,
                                  @Valid @RequestBody RouteStopRequest req) {
        return service.updateStop(routeId, stopId, req);
    }

    /**
     * Xóa một điểm dừng khỏi tuyến. Thứ tự các điểm còn lại được tự động sắp xếp lại.
     *
     * @param routeId id tuyến đường
     * @param stopId  id điểm dừng cần xóa
     * @return tuyến sau khi xóa điểm dừng
     */
    @DeleteMapping("/{routeId}/stops/{stopId}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public RouteDetail deleteStop(@PathVariable Long routeId, @PathVariable Long stopId) {
        return service.deleteStop(routeId, stopId);
    }

    /**
     * Xóa tuyến đường. Nếu không có chuyến nào tham chiếu → xóa hẳn; ngược lại → lưu trữ (archive).
     *
     * @param id id tuyến cần xóa
     * @return map chứa {@code deleted=true} nếu xóa hẳn, {@code archived=true} nếu chỉ lưu trữ
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FLEET_ROUTE_COORDINATOR')")
    public java.util.Map<String, Object> delete(@PathVariable Long id) {
        boolean removed = service.delete(id);
        return java.util.Map.of("deleted", removed, "archived", !removed);
    }
}
