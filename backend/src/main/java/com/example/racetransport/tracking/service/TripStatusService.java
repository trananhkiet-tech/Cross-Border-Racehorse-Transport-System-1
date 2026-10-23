package com.example.racetransport.tracking.service;

import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.tracking.entity.TrackingEvent;
import com.example.racetransport.tracking.entity.TrackingEventType;
import com.example.racetransport.tracking.entity.TripStatusHistory;
import com.example.racetransport.tracking.repository.TrackingEventRepository;
import com.example.racetransport.tracking.repository.TripStatusHistoryRepository;
import com.example.racetransport.route.entity.RouteStop;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.user.entity.AppUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Điểm duy nhất thay đổi trạng thái chuyến: kiểm tra transition, ghi TripStatusHistory
 * và TrackingEvent tương ứng (mọi thay đổi quan trọng đều có lịch sử).
 */
@Service
public class TripStatusService {

    private final TripStatusHistoryRepository histories;
    private final TrackingEventRepository events;
    private final Clock clock;

    public TripStatusService(TripStatusHistoryRepository histories, TrackingEventRepository events, Clock clock) {
        this.histories = histories;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void changeStatus(TransportationTrip trip, TripStatus to, AppUser actor, String note) {
        TripStatus from = trip.getStatus();
        if (!TripStateMachine.canTransition(from, to)) {
            throw new InvalidStateException("Không thể chuyển chuyến " + trip.getTripNumber() + " từ \""
                    + from.getLabel() + "\" sang \"" + to.getLabel() + "\"");
        }
        trip.setStatus(to);
        TripStatusHistory h = new TripStatusHistory();
        h.setTrip(trip);
        h.setPreviousStatus(from);
        h.setNewStatus(to);
        h.setChangedBy(actor);
        h.setChangedAt(LocalDateTime.now(clock));
        h.setNote(note);
        histories.save(h);
    }

    /** Ghi lịch sử lần tạo chuyến. */
    @Transactional
    public void recordCreated(TransportationTrip trip, AppUser actor) {
        TripStatusHistory h = new TripStatusHistory();
        h.setTrip(trip);
        h.setNewStatus(trip.getStatus());
        h.setChangedBy(actor);
        h.setChangedAt(LocalDateTime.now(clock));
        h.setNote("Lập chuyến");
        histories.save(h);
    }

    @Transactional
    public TrackingEvent recordEvent(TransportationTrip trip, TrackingEventType type, RouteStop stop, String location,
                                     BigDecimal lat, BigDecimal lng, Integer delayMinutes, String note,
                                     LocalDateTime eventTime, AppUser actor) {
        TrackingEvent e = new TrackingEvent();
        e.setTrip(trip);
        e.setRouteStop(stop);
        e.setEventType(type);
        e.setEventTime(eventTime == null ? LocalDateTime.now(clock) : eventTime);
        e.setLocation(location);
        e.setLatitude(lat);
        e.setLongitude(lng);
        e.setTripStatus(trip.getStatus());
        e.setDelayMinutes(delayMinutes);
        e.setNote(note);
        e.setRecordedBy(actor);
        if (location != null && !location.isBlank()) {
            trip.setCurrentLocation(location);
        }
        if (stop != null) {
            trip.setCurrentStop(stop);
        }
        return events.save(e);
    }
}
