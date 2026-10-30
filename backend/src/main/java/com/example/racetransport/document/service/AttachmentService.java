package com.example.racetransport.document.service;

import com.example.racetransport.common.exception.ForbiddenException;
import com.example.racetransport.common.exception.NotFoundException;
import com.example.racetransport.document.dto.DocumentDtos.AttachmentResponse;
import com.example.racetransport.document.entity.Attachment;
import com.example.racetransport.document.entity.AttachmentRefType;
import com.example.racetransport.document.entity.StoredFile;
import com.example.racetransport.document.repository.AttachmentRepository;
import com.example.racetransport.handover.repository.HandoverRepository;
import com.example.racetransport.health.dto.HealthDtos.AttachmentRef;
import com.example.racetransport.health.repository.ArrivalConditionCheckRepository;
import com.example.racetransport.health.repository.HealthObservationRepository;
import com.example.racetransport.health.repository.VeterinaryConsultationRepository;
import com.example.racetransport.incident.repository.IncidentRepository;
import com.example.racetransport.security.UserPrincipal;
import com.example.racetransport.tracking.repository.TrackingEventRepository;
import com.example.racetransport.trip.entity.TransportationTrip;
import com.example.racetransport.trip.service.TripAccess;
import com.example.racetransport.user.repository.AppUserRepository;
import java.util.List;
import org.springframework.core.io.Resource;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Ảnh/tệp đính kèm cho quan sát sức khỏe, sự cố, bàn giao, kiểm tra khi đến, tư vấn thú y.
 * Quyền được suy ra từ chuyến chứa đối tượng tham chiếu.
 */
@Service
public class AttachmentService {

    private final AttachmentRepository attachments;
    private final FileStorageService storage;
    private final HealthObservationRepository observations;
    private final IncidentRepository incidents;
    private final HandoverRepository handovers;
    private final ArrivalConditionCheckRepository arrivalChecks;
    private final VeterinaryConsultationRepository consultations;
    private final TrackingEventRepository trackingEvents;
    private final TripAccess tripAccess;
    private final AppUserRepository users;

    public AttachmentService(AttachmentRepository attachments, FileStorageService storage,
                             HealthObservationRepository observations, IncidentRepository incidents,
                             HandoverRepository handovers, ArrivalConditionCheckRepository arrivalChecks,
                             VeterinaryConsultationRepository consultations, TrackingEventRepository trackingEvents,
                             TripAccess tripAccess, AppUserRepository users) {
        this.attachments = attachments;
        this.storage = storage;
        this.observations = observations;
        this.incidents = incidents;
        this.handovers = handovers;
        this.arrivalChecks = arrivalChecks;
        this.consultations = consultations;
        this.trackingEvents = trackingEvents;
        this.tripAccess = tripAccess;
        this.users = users;
    }

    @Transactional
    public AttachmentResponse upload(UserPrincipal actor, AttachmentRefType type, Long refId, String caption,
                                     MultipartFile file) {
        TransportationTrip trip = tripOf(type, refId);
        boolean allowed = (actor.isDriverEscort() && tripAccess.isAssigned(actor, trip))
                || actor.isManager() || (actor.isSpecialist() && type == AttachmentRefType.VET_CONSULTATION);
        if (!allowed) {
            throw new ForbiddenException("Bạn không có quyền đính kèm tệp cho đối tượng này");
        }
        StoredFile f = storage.store(file, users.getReferenceById(actor.getId()));
        Attachment a = new Attachment();
        a.setFile(f);
        a.setRefType(type);
        a.setRefId(refId);
        a.setCaption(caption == null || caption.isBlank() ? null
                : caption.length() > 255 ? caption.substring(0, 255) : caption);
        attachments.save(a);
        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(UserPrincipal actor, AttachmentRefType type, Long refId) {
        TransportationTrip trip = tripOf(type, refId);
        if (!tripAccess.canView(actor, trip)) {
            throw new ForbiddenException("Bạn không có quyền xem tệp đính kèm này");
        }
        return attachments.findByRefTypeAndRefIdOrderByIdAsc(type, refId).stream().map(AttachmentService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StoredFile fileOf(UserPrincipal actor, Long attachmentId) {
        Attachment a = attachments.findById(attachmentId).orElseThrow(() -> new NotFoundException("Không tìm thấy tệp"));
        TransportationTrip trip = tripOf(a.getRefType(), a.getRefId());
        if (!tripAccess.canView(actor, trip)) {
            throw new ForbiddenException("Bạn không có quyền xem tệp này");
        }
        // open-in-view=false: nạp proxy LAZY ngay trong transaction để controller đọc được tên/loại tệp
        return Hibernate.unproxy(a.getFile()) instanceof StoredFile f ? f : a.getFile();
    }

    public Resource open(StoredFile f) {
        return storage.open(f);
    }

    /** Tham chiếu gọn để nhúng vào DTO khác (đã kiểm tra quyền ở service gọi). */
    public List<AttachmentRef> refs(AttachmentRefType type, Long refId) {
        return attachments.findByRefTypeAndRefIdOrderByIdAsc(type, refId).stream()
                .map(a -> new AttachmentRef(a.getId(), a.getFile().getOriginalFilename(), a.getFile().getContentType()))
                .toList();
    }

    private TransportationTrip tripOf(AttachmentRefType type, Long refId) {
        return switch (type) {
            case HEALTH_OBSERVATION -> observations.findById(refId).map(o -> o.getTrip())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy quan sát"));
            case INCIDENT -> incidents.findById(refId).map(i -> i.getTrip())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy sự cố"));
            case HANDOVER -> handovers.findById(refId).map(h -> h.getTrip())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy biên bản bàn giao"));
            case ARRIVAL_CHECK -> arrivalChecks.findById(refId).map(c -> c.getTrip())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy kiểm tra khi đến"));
            case VET_CONSULTATION -> consultations.findById(refId)
                    .map(v -> v.getTrip() != null ? v.getTrip() : v.getIncident() != null ? v.getIncident().getTrip() : null)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy tư vấn thú y hoặc tư vấn không gắn chuyến"));
            case TRACKING_EVENT -> trackingEvents.findById(refId).map(e -> e.getTrip())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện hành trình"));
        };
    }

    public static AttachmentResponse toResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getRefType(), a.getRefId(), a.getCaption(),
                a.getFile().getOriginalFilename(), a.getFile().getContentType(), a.getFile().getSizeBytes(),
                a.getCreatedAt());
    }
}
