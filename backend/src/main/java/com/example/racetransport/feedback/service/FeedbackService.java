package com.example.racetransport.feedback.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.feedback.dto.FeedbackDtos.DeliveredTripItem;
import com.example.racetransport.feedback.dto.FeedbackDtos.FeedbackRequest;
import com.example.racetransport.feedback.dto.FeedbackDtos.FeedbackResponse;
import com.example.racetransport.feedback.dto.FeedbackDtos.FeedbackSummary;
import com.example.racetransport.feedback.dto.FeedbackDtos.RouteScore;
import com.example.racetransport.feedback.entity.TripFeedback;
import com.example.racetransport.feedback.repository.TripFeedbackRepository;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.TransportationRequestRepository;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.entity.TripStatus;
import com.example.racetransport.trip.repository.TransportationTripRepository;
import com.example.racetransport.user.entity.Role;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đánh giá chuyến: chỉ chủ yêu cầu, chỉ khi chuyến DELIVERED, sửa được trong 7 ngày.
 * Điểm tổng ≤ 2 thì báo Manager.
 */
@Service
public class FeedbackService {

    public static final int EDIT_DAYS = 7;
    public static final double LOW_SCORE = 2.0;

    private final TripFeedbackRepository feedbacks;
    private final TransportationTripRepository trips;
    private final TransportationRequestRepository requests;
    private final NotificationService notifications;
    private final Clock clock;

    public FeedbackService(TripFeedbackRepository feedbacks, TransportationTripRepository trips,
                           TransportationRequestRepository requests, NotificationService notifications, Clock clock) {
        this.feedbacks = feedbacks;
        this.trips = trips;
        this.requests = requests;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public FeedbackResponse submit(UserPrincipal actor, Long tripId, FeedbackRequest req) {
        Guard.state(actor.isCustomer(), "Chỉ khách hàng mới đánh giá chuyến");
        TransportationTrip trip = trips.findById(tripId).orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến"));
        if (!trip.getRequest().getCustomer().getId().equals(actor.getId())) {
            throw new ForbiddenException("Bạn không có quyền đánh giá chuyến này");
        }
        Guard.state(trip.getStatus() == TripStatus.DELIVERED, "Chỉ đánh giá được chuyến đã giao");
        LocalDateTime now = LocalDateTime.now(clock);
        TripFeedback f = feedbacks.findByTripId(tripId).orElse(null);
        if (f == null) {
            f = new TripFeedback();
            f.setTripId(tripId);
            f.setCustomerId(actor.getId());
            f.setCreatedAt(now);
        } else {
            Guard.state(!now.isAfter(f.getCreatedAt().plusDays(EDIT_DAYS)),
                    "Đã quá " + EDIT_DAYS + " ngày, không thể sửa đánh giá");
        }
        f.setPunctuality(req.punctuality());
        f.setCare(req.care());
        f.setCommunication(req.communication());
        f.setComment(req.comment() == null || req.comment().isBlank() ? null : req.comment().trim());
        f.setUpdatedAt(now);
        f = feedbacks.save(f);
        if (f.overall() <= LOW_SCORE) {
            notifications.notifyRole(Role.LOGISTICS_MANAGER, NotificationType.WARNING,
                    "Khách đánh giá thấp chuyến " + trip.getTripNumber(),
                    "Điểm tổng " + f.overall() + "/5. Nên liên hệ khách để xử lý.",
                    NotificationService.REF_TRIP, trip.getId());
        }
        return toResponse(f, now);
    }

    @Transactional(readOnly = true)
    public FeedbackResponse get(UserPrincipal actor, Long tripId) {
        TransportationTrip trip = trips.findById(tripId).orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến"));
        if (actor.isCustomer() && !trip.getRequest().getCustomer().getId().equals(actor.getId())) {
            throw new ForbiddenException("Bạn không có quyền xem đánh giá này");
        }
        TripFeedback f = feedbacks.findByTripId(tripId)
                .orElseThrow(() -> new NotFoundException("Chuyến này chưa có đánh giá"));
        return toResponse(f, LocalDateTime.now(clock));
    }

    /** Các chuyến đã giao của khách, kèm đánh giá. */
    @Transactional(readOnly = true)
    public List<DeliveredTripItem> mine(UserPrincipal actor) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<TransportationTrip> delivered = new ArrayList<>();
        for (TransportationRequest r : requests.findByCustomerIdOrderByCreatedAtDesc(actor.getId())) {
            for (TransportationTrip t : trips.findByRequestIdOrderByPlannedDepartureAsc(r.getId())) {
                if (t.getStatus() == TripStatus.DELIVERED) {
                    delivered.add(t);
                }
            }
        }
        Map<Long, TripFeedback> byTrip = feedbacks.findByTripIdIn(delivered.stream().map(TransportationTrip::getId).toList())
                .stream().collect(Collectors.toMap(TripFeedback::getTripId, x -> x));
        return delivered.stream().map(t -> new DeliveredTripItem(t.getId(), t.getTripNumber(),
                t.getRequest().getRequestNumber(), t.getActualArrival(),
                byTrip.containsKey(t.getId()) ? toResponse(byTrip.get(t.getId()), now) : null)).toList();
    }

    /** Điểm trung bình theo tuyến (Manager). */
    @Transactional(readOnly = true)
    public FeedbackSummary summary() {
        List<TripFeedback> all = feedbacks.findAll();
        Map<Long, TransportationTrip> tripById = trips.findAllById(all.stream().map(TripFeedback::getTripId).toList())
                .stream().collect(Collectors.toMap(TransportationTrip::getId, t -> t));
        Map<Long, List<TripFeedback>> byRoute = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        for (TripFeedback f : all) {
            TransportationTrip t = tripById.get(f.getTripId());
            if (t == null || t.getRoute() == null) {
                continue;
            }
            byRoute.computeIfAbsent(t.getRoute().getId(), k -> new ArrayList<>()).add(f);
            names.put(t.getRoute().getId(), t.getRoute().getName());
        }
        List<RouteScore> routes = byRoute.entrySet().stream().map(e -> score(e.getKey(), names.get(e.getKey()), e.getValue()))
                .sorted((a, b) -> Double.compare(a.overall(), b.overall())).toList();
        double overall = all.isEmpty() ? 0 : round1(all.stream().mapToDouble(TripFeedback::overall).average().orElse(0));
        return new FeedbackSummary(all.size(), overall, routes);
    }

    static RouteScore score(Long routeId, String name, List<TripFeedback> list) {
        return new RouteScore(routeId, name, list.size(),
                round1(list.stream().mapToInt(TripFeedback::getPunctuality).average().orElse(0)),
                round1(list.stream().mapToInt(TripFeedback::getCare).average().orElse(0)),
                round1(list.stream().mapToInt(TripFeedback::getCommunication).average().orElse(0)),
                round1(list.stream().mapToDouble(TripFeedback::overall).average().orElse(0)));
    }

    static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    FeedbackResponse toResponse(TripFeedback f, LocalDateTime now) {
        return new FeedbackResponse(f.getId(), f.getTripId(), f.getPunctuality(), f.getCare(), f.getCommunication(),
                f.overall(), f.getComment(), f.getCreatedAt(), f.getUpdatedAt(),
                !now.isAfter(f.getCreatedAt().plusDays(EDIT_DAYS)));
    }
}
