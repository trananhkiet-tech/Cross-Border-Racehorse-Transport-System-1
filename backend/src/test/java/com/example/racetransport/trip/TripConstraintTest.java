package com.example.racetransport.trip;

import static org.junit.jupiter.api.Assertions.*;

import com.example.racetransport.fleet.entity.VehicleStatus;
import com.example.racetransport.fleet.service.VehicleService;
import com.example.racetransport.trip.entity.TripStatus;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests kiểm tra tính toàn vẹn của ràng buộc nghiệp vụ trong quản lý chuyến vận chuyển.
 *
 * <p>Chủ yếu kiểm tra các bất biến (invariants) của trạng thái chuyến để đảm bảo
 * Fleet & Route Coordinator không phân công sai xe hoặc lên lịch xung đột.</p>
 *
 * @author dangbao-hub
 */
class TripConstraintTest {

    // ─── TripStatus invariants ─────────────────────────────────────────────────

    @Test
    @DisplayName("OPEN_TRIP không được chứa DELIVERED và CANCELLED")
    void openTrip_excludesTerminalStatuses() {
        EnumSet<TripStatus> open = VehicleService.OPEN_TRIP;

        assertFalse(open.contains(TripStatus.DELIVERED),  "DELIVERED phải nằm ngoài OPEN_TRIP");
        assertFalse(open.contains(TripStatus.CANCELLED),  "CANCELLED phải nằm ngoài OPEN_TRIP");
    }

    @Test
    @DisplayName("OPEN_TRIP chứa đầy đủ các trạng thái đang hoạt động")
    void openTrip_containsActiveStatuses() {
        EnumSet<TripStatus> open = VehicleService.OPEN_TRIP;

        assertTrue(open.contains(TripStatus.SCHEDULED));
        assertTrue(open.contains(TripStatus.PREPARING));
        assertTrue(open.contains(TripStatus.IN_TRANSIT));
        assertTrue(open.contains(TripStatus.AT_BORDER));
        assertTrue(open.contains(TripStatus.CUSTOMS_CLEARANCE));
        assertTrue(open.contains(TripStatus.QUARANTINE));
        assertTrue(open.contains(TripStatus.DELAYED));
        assertTrue(open.contains(TripStatus.ARRIVED));
        assertTrue(open.contains(TripStatus.HANDOVER_PENDING));
    }

    @Test
    @DisplayName("OPEN_TRIP + terminal = tất cả TripStatus")
    void openTrip_plusTerminal_equalsAll() {
        EnumSet<TripStatus> open = VehicleService.OPEN_TRIP;
        Set<TripStatus> all = EnumSet.allOf(TripStatus.class);

        EnumSet<TripStatus> union = EnumSet.copyOf(open);
        union.add(TripStatus.DELIVERED);
        union.add(TripStatus.CANCELLED);

        assertEquals(all, union, "OPEN_TRIP cộng DELIVERED, CANCELLED phải bằng toàn bộ TripStatus");
    }

    // ─── TripStatus label ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Mỗi TripStatus có nhãn tiếng Việt không rỗng")
    void tripStatus_allHaveNonEmptyLabel() {
        for (TripStatus s : TripStatus.values()) {
            assertNotNull(s.getLabel(),    s.name() + " thiếu nhãn");
            assertFalse(s.getLabel().isBlank(), s.name() + " nhãn rỗng");
        }
    }

    @Test
    @DisplayName("TripStatus.DELIVERED có nhãn 'Đã giao'")
    void tripStatus_deliveredLabel() {
        assertEquals("Đã giao", TripStatus.DELIVERED.getLabel());
    }

    @Test
    @DisplayName("TripStatus.CANCELLED có nhãn 'Đã hủy'")
    void tripStatus_cancelledLabel() {
        assertEquals("Đã hủy", TripStatus.CANCELLED.getLabel());
    }

    // ─── VehicleStatus logic ───────────────────────────────────────────────────

    @Test
    @DisplayName("VehicleStatus AVAILABLE, IN_USE, MAINTENANCE, RETIRED đều tồn tại")
    void vehicleStatus_allExpectedValuesExist() {
        Set<VehicleStatus> values = EnumSet.allOf(VehicleStatus.class);

        assertTrue(values.contains(VehicleStatus.AVAILABLE));
        assertTrue(values.contains(VehicleStatus.IN_USE));
        assertTrue(values.contains(VehicleStatus.MAINTENANCE));
        assertTrue(values.contains(VehicleStatus.RETIRED));
    }

    @Test
    @DisplayName("VehicleStatus không có trạng thái nào tên rỗng")
    void vehicleStatus_allHaveNonEmptyName() {
        for (VehicleStatus s : VehicleStatus.values()) {
            assertFalse(s.name().isBlank(), "VehicleStatus tên rỗng bị phát hiện: " + s);
        }
    }

    // ─── Scheduling logic ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Tổng số trạng thái TripStatus khớp với thiết kế (13 trạng thái)")
    void tripStatus_totalCount() {
        assertEquals(13, TripStatus.values().length,
                "Số lượng TripStatus phải đúng 13 theo thiết kế nghiệp vụ");
    }

    @Test
    @DisplayName("OPEN_TRIP gồm đúng 11 trạng thái (tất cả trừ DELIVERED và CANCELLED)")
    void openTrip_correctSize() {
        assertEquals(11, VehicleService.OPEN_TRIP.size(),
                "OPEN_TRIP phải có 11 trạng thái");
    }
}
