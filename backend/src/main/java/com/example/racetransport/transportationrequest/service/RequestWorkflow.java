package com.example.racetransport.transportationrequest.service;

import static com.example.racetransport.transportationrequest.entity.RequestStatus.*;

import com.example.racetransport.common.exception.InvalidStateException;
import com.example.racetransport.transportationrequest.entity.RequestStatus;
import com.example.racetransport.transportationrequest.entity.RequestStatusHistory;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.repository.RequestStatusHistoryRepository;
import com.example.racetransport.user.entity.AppUser;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Máy trạng thái của Transportation Request. Mọi thay đổi trạng thái đều đi qua đây
 * để kiểm tra transition hợp lệ và ghi request_status_history.
 */
@Component
public class RequestWorkflow {

    private static final Map<RequestStatus, Set<RequestStatus>> TRANSITIONS = new EnumMap<>(RequestStatus.class);

    static {
        TRANSITIONS.put(DRAFT, EnumSet.of(SUBMITTED, CANCELLED));
        TRANSITIONS.put(SUBMITTED, EnumSet.of(UNDER_DOCUMENT_REVIEW, INFORMATION_REQUIRED, REJECTED, CANCELLED));
        TRANSITIONS.put(UNDER_DOCUMENT_REVIEW, EnumSet.of(COMPLIANCE_REVIEW, INFORMATION_REQUIRED, REJECTED, CANCELLED));
        TRANSITIONS.put(INFORMATION_REQUIRED, EnumSet.of(SUBMITTED, REJECTED, CANCELLED));
        TRANSITIONS.put(COMPLIANCE_REVIEW, EnumSet.of(PENDING_MANAGER_APPROVAL, INFORMATION_REQUIRED, REJECTED, CANCELLED));
        TRANSITIONS.put(PENDING_MANAGER_APPROVAL, EnumSet.of(APPROVED, REJECTED, INFORMATION_REQUIRED, CANCELLED));
        TRANSITIONS.put(APPROVED, EnumSet.of(QUOTE_SENT, CANCELLED));
        TRANSITIONS.put(QUOTE_SENT, EnumSet.of(QUOTE_SENT, QUOTE_ACCEPTED, QUOTE_REJECTED, CANCELLED));
        TRANSITIONS.put(QUOTE_REJECTED, EnumSet.of(QUOTE_SENT, CANCELLED));
        TRANSITIONS.put(QUOTE_ACCEPTED, EnumSet.of(READY_FOR_PLANNING));
        TRANSITIONS.put(READY_FOR_PLANNING, EnumSet.of(TRIP_PLANNED));
        TRANSITIONS.put(TRIP_PLANNED, EnumSet.of(TRIP_PLANNED, COMPLETED, READY_FOR_PLANNING));
        TRANSITIONS.put(REJECTED, EnumSet.noneOf(RequestStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(RequestStatus.class));
        TRANSITIONS.put(COMPLETED, EnumSet.noneOf(RequestStatus.class));
    }

    /** Trạng thái khách còn được sửa nội dung yêu cầu. */
    public static final Set<RequestStatus> EDITABLE = EnumSet.of(DRAFT, INFORMATION_REQUIRED);

    /** Trạng thái đang trong quy trình xét duyệt hồ sơ. */
    public static final Set<RequestStatus> IN_REVIEW = EnumSet.of(SUBMITTED, UNDER_DOCUMENT_REVIEW,
            COMPLIANCE_REVIEW, PENDING_MANAGER_APPROVAL);

    /** Trạng thái kết thúc. */
    public static final Set<RequestStatus> TERMINAL = EnumSet.of(REJECTED, CANCELLED, COMPLETED);

    /** Yêu cầu "đang hoạt động" theo góc nhìn khách hàng. */
    public static final Set<RequestStatus> ACTIVE = EnumSet.complementOf(EnumSet.of(REJECTED, CANCELLED, COMPLETED, DRAFT));

    private final RequestStatusHistoryRepository history;
    private final Clock clock;

    public RequestWorkflow(RequestStatusHistoryRepository history, Clock clock) {
        this.history = history;
        this.clock = clock;
    }

    public static boolean canTransition(RequestStatus from, RequestStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * Chuyển trạng thái và ghi lịch sử. Ném 409 nếu transition không hợp lệ.
     */
    public void transition(TransportationRequest request, RequestStatus to, AppUser actor, String note) {
        RequestStatus from = request.getStatus();
        if (!canTransition(from, to)) {
            throw new InvalidStateException("Không thể chuyển yêu cầu " + request.getRequestNumber()
                    + " từ \"" + from.getLabel() + "\" sang \"" + to.getLabel() + "\"");
        }
        request.setStatus(to);
        LocalDateTime now = LocalDateTime.now(clock);
        if (to == SUBMITTED) {
            request.setSubmittedAt(now);
        } else if (to == APPROVED) {
            request.setApprovedAt(now);
        } else if (to == COMPLETED) {
            request.setCompletedAt(now);
        }
        record(request, from, to, actor, note, now);
    }

    /** Ghi lịch sử lần tạo (from = null). */
    public void recordCreation(TransportationRequest request, AppUser actor) {
        record(request, null, request.getStatus(), actor, "Tạo yêu cầu", LocalDateTime.now(clock));
    }

    private void record(TransportationRequest request, RequestStatus from, RequestStatus to, AppUser actor,
                        String note, LocalDateTime at) {
        RequestStatusHistory h = new RequestStatusHistory();
        h.setRequest(request);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setChangedBy(actor);
        h.setNote(note);
        h.setChangedAt(at);
        history.save(h);
    }
}
