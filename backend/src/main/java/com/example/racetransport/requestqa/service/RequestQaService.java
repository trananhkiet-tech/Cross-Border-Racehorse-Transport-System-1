package com.example.racetransport.requestqa.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.common.util.Guard;
import com.example.racetransport.notification.entity.NotificationType;
import com.example.racetransport.notification.service.NotificationService;
import com.example.racetransport.requestqa.dto.QaDtos.AnswerRequest;
import com.example.racetransport.requestqa.dto.QaDtos.AskRequest;
import com.example.racetransport.requestqa.dto.QaDtos.QuestionResponse;
import com.example.racetransport.requestqa.entity.RequestQuestion;
import com.example.racetransport.requestqa.repository.RequestQuestionRepository;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.transportationrequest.entity.TransportationRequest;
import com.example.racetransport.transportationrequest.service.RequestAccess;
import com.example.racetransport.user.entity.AppUser;
import com.example.racetransport.user.entity.Role;
import com.example.racetransport.user.repository.AppUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hỏi đáp trên yêu cầu: khách ↔ nhân viên (Manager/Specialist). Người hỏi là khách thì nhân viên trả lời,
 * người hỏi là nhân viên thì khách trả lời. Chỉ người xem được yêu cầu mới tham gia.
 */
@Service
public class RequestQaService {

    private final RequestQuestionRepository questions;
    private final RequestAccess access;
    private final AppUserRepository users;
    private final NotificationService notifications;
    private final Clock clock;

    public RequestQaService(RequestQuestionRepository questions, RequestAccess access, AppUserRepository users,
                            NotificationService notifications, Clock clock) {
        this.questions = questions;
        this.access = access;
        this.users = users;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public QuestionResponse ask(UserPrincipal actor, Long requestId, AskRequest req) {
        Guard.state(actor.isCustomer() || actor.isManager() || actor.isSpecialist(),
                "Vai trò của bạn không đặt câu hỏi trên yêu cầu");
        TransportationRequest r = access.loadVisible(actor, requestId);
        RequestQuestion q = new RequestQuestion();
        q.setRequestId(r.getId());
        q.setAskedBy(actor.getId());
        q.setQuestion(req.question().trim());
        q.setCreatedAt(LocalDateTime.now(clock));
        q = questions.save(q);
        if (actor.isCustomer()) {
            notifications.notifyRole(Role.TRANSPORT_SPECIALIST, NotificationType.ACTION_REQUIRED,
                    "Khách hỏi về yêu cầu " + r.getRequestNumber(), q.getQuestion(),
                    NotificationService.REF_REQUEST, r.getId());
        } else {
            notifications.notify(r.getCustomer(), NotificationType.ACTION_REQUIRED,
                    "Cần bạn trả lời về yêu cầu " + r.getRequestNumber(), q.getQuestion(),
                    NotificationService.REF_REQUEST, r.getId());
        }
        return toResponse(actor, q, r);
    }

    @Transactional
    public QuestionResponse answer(UserPrincipal actor, Long questionId, AnswerRequest req) {
        RequestQuestion q = questions.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi"));
        TransportationRequest r = access.loadVisible(actor, q.getRequestId());
        Guard.state(q.getAnswer() == null, "Câu hỏi đã được trả lời");
        AppUser asker = users.findById(q.getAskedBy()).orElseThrow(() -> new NotFoundException("Không tìm thấy người hỏi"));
        if (!canAnswer(actor, asker)) {
            throw new ForbiddenException("Bạn không thể trả lời câu hỏi này");
        }
        q.setAnswer(req.answer().trim());
        q.setAnsweredBy(actor.getId());
        q.setAnsweredAt(LocalDateTime.now(clock));
        questions.save(q);
        notifications.notify(asker, NotificationType.INFO, "Đã có trả lời cho câu hỏi về yêu cầu " + r.getRequestNumber(),
                q.getAnswer(), NotificationService.REF_REQUEST, r.getId());
        return toResponse(actor, q, r);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listByRequest(UserPrincipal actor, Long requestId) {
        TransportationRequest r = access.loadVisible(actor, requestId);
        return questions.findByRequestIdOrderByCreatedAtAsc(requestId).stream().map(q -> toResponse(actor, q, r)).toList();
    }

    /** Hộp thư: câu hỏi chưa trả lời mà người dùng có thể trả lời. */
    @Transactional(readOnly = true)
    public List<QuestionResponse> inbox(UserPrincipal actor) {
        return questions.findByAnswerIsNullOrderByCreatedAtAsc().stream()
                .map(q -> {
                    TransportationRequest r = access.load(q.getRequestId());
                    return access.canView(actor, r) ? toResponse(actor, q, r) : null;
                })
                .filter(x -> x != null && x.canAnswer())
                .toList();
    }

    private boolean canAnswer(UserPrincipal actor, AppUser asker) {
        if (asker.getRole() == Role.CUSTOMER) {
            return actor.isManager() || actor.isSpecialist();
        }
        return actor.isCustomer();
    }

    private QuestionResponse toResponse(UserPrincipal actor, RequestQuestion q, TransportationRequest r) {
        AppUser asker = users.findById(q.getAskedBy()).orElse(null);
        AppUser answerer = q.getAnsweredBy() == null ? null : users.findById(q.getAnsweredBy()).orElse(null);
        boolean can = q.getAnswer() == null && asker != null && canAnswer(actor, asker);
        return new QuestionResponse(q.getId(), r.getId(), r.getRequestNumber(), q.getQuestion(),
                asker == null ? "—" : asker.getFullName(), asker == null ? null : asker.getRole(), q.getCreatedAt(),
                q.getAnswer(), answerer == null ? null : answerer.getFullName(), q.getAnsweredAt(), can);
    }
}
