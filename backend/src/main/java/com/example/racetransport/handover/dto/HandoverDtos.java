package com.example.racetransport.handover.dto;

import com.example.racetransport.handover.entity.HandoverStatus;
import com.example.racetransport.health.dto.HealthDtos.AttachmentRef;
import com.example.racetransport.health.entity.HorseCondition;
import com.example.racetransport.trip.entity.TripStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class HandoverDtos {

    private HandoverDtos() { }

    public record HandoverRequest(
            @NotNull Long tripId,
            @NotNull Long horseId,
            @Size(max = 255) String destination,
            LocalDateTime handoverTime,
            @NotBlank @Size(max = 150) String receiverName,
            @Size(max = 150) String receiverContact,
            @NotNull HorseCondition condition,
            Boolean documentsHandedOver,
            @Size(max = 2000) String notes) { }

    /** Khách hàng (người nhận) xác nhận hoặc phản hồi có vấn đề. */
    public record ConfirmHandoverRequest(@NotNull Boolean accepted, @Size(max = 1000) String comment) { }

    public record HandoverResponse(Long id, String handoverNumber, Long tripId, String tripNumber, TripStatus tripStatus,
                                   Long requestId, String requestNumber, Long horseId, String horseName,
                                   String microchipNumber, String destination, LocalDateTime handoverTime,
                                   String receiverName, String receiverContact, HorseCondition condition,
                                   boolean documentsHandedOver, String notes, HandoverStatus status,
                                   Long arrivalCheckId, HorseCondition arrivalCondition, String createdBy,
                                   String confirmedBy, LocalDateTime confirmedAt, String customerComment,
                                   List<AttachmentRef> evidence, List<String> allowedActions,
                                   LocalDateTime updatedAt) { }
}
