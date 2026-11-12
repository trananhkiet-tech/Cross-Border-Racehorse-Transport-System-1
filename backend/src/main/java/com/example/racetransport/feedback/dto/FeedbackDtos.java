package com.example.racetransport.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public final class FeedbackDtos {

    private FeedbackDtos() { }

    public record FeedbackRequest(
            @NotNull @Min(1) @Max(5) Integer punctuality,
            @NotNull @Min(1) @Max(5) Integer care,
            @NotNull @Min(1) @Max(5) Integer communication,
            @Size(max = 1000) String comment) { }

    public record FeedbackResponse(Long id, Long tripId, int punctuality, int care, int communication,
                                   double overall, String comment, LocalDateTime createdAt,
                                   LocalDateTime updatedAt, boolean editable) { }

    /** Chuyến đã giao của khách, kèm đánh giá (nếu có). */
    public record DeliveredTripItem(Long tripId, String tripNumber, String requestNumber,
                                    LocalDateTime deliveredAt, FeedbackResponse feedback) { }

    public record RouteScore(Long routeId, String routeName, long count, double punctuality, double care,
                             double communication, double overall) { }

    public record FeedbackSummary(long totalFeedbacks, double overall, List<RouteScore> routes) { }
}
