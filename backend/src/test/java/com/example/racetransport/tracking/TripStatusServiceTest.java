package com.example.racetransport.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.support.TestData;
import com.example.racetransport.tracking.entity.TripStatusHistory;
import com.example.racetransport.tracking.repository.TrackingEventRepository;
import com.example.racetransport.tracking.repository.TripStatusHistoryRepository;
import com.example.racetransport.tracking.service.TripStateMachine;
import com.example.racetransport.tracking.service.TripStatusService;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Mọi thay đổi trạng thái chuyến phải hợp lệ và được lưu TripStatusHistory. */
class TripStatusServiceTest {

    private TripStatusHistoryRepository histories;
    private TripStatusService service;
    private AppUser driver;

    @BeforeEach
    void setUp() {
        histories = mock(TripStatusHistoryRepository.class);
        service = new TripStatusService(histories, mock(TrackingEventRepository.class), TestData.clock());
        driver = TestData.user(5, Role.DRIVER_ESCORT, "Le Hoang Nam");
    }

    private static TransportationTrip trip(TripStatus status) {
        TransportationTrip t = new TransportationTrip();
        t.setId(94L);
        t.setTripNumber("TRIP-2026-0094");
        t.setStatus(status);
        return t;
    }

    @Test
    void statusChangeIsRecordedInHistory() {
        TransportationTrip t = trip(TripStatus.IN_TRANSIT);
        service.changeStatus(t, TripStatus.AT_BORDER, driver, "Đến cửa khẩu");
        assertEquals(TripStatus.AT_BORDER, t.getStatus());
        ArgumentCaptor<TripStatusHistory> captor = ArgumentCaptor.forClass(TripStatusHistory.class);
        verify(histories).save(captor.capture());
        assertEquals(TripStatus.IN_TRANSIT, captor.getValue().getPreviousStatus());
        assertEquals(TripStatus.AT_BORDER, captor.getValue().getNewStatus());
        assertEquals(driver, captor.getValue().getChangedBy());
    }

    @Test
    void cannotSkipDirectlyFromScheduledToDelivered() {
        TransportationTrip t = trip(TripStatus.SCHEDULED);
        assertThrows(InvalidStateException.class, () -> service.changeStatus(t, TripStatus.DELIVERED, driver, null));
        verify(histories, never()).save(any());
    }

    @Test
    void deliveredOnlyAfterHandoverPending() {
        assertFalse(TripStateMachine.canTransition(TripStatus.ARRIVED, TripStatus.DELIVERED));
        assertTrue(TripStateMachine.canTransition(TripStatus.ARRIVED, TripStatus.HANDOVER_PENDING));
        assertTrue(TripStateMachine.canTransition(TripStatus.HANDOVER_PENDING, TripStatus.DELIVERED));
    }

    @Test
    void cannotStartWithoutReadyForDeparture() {
        assertFalse(TripStateMachine.canTransition(TripStatus.SCHEDULED, TripStatus.IN_TRANSIT));
        assertTrue(TripStateMachine.canTransition(TripStatus.READY_FOR_DEPARTURE, TripStatus.IN_TRANSIT));
    }
}
