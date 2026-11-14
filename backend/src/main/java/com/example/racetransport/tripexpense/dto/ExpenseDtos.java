package com.example.racetransport.tripexpense.dto;

import com.example.racetransport.tripexpense.entity.ExpenseCategory;
import com.example.racetransport.tripexpense.entity.ExpenseStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ExpenseDtos {

    private ExpenseDtos() { }

    public record ExpenseRequest(@NotNull ExpenseCategory category,
                                 @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                 @Size(max = 500) String description) { }

    public record ReviewRequest(@NotNull Boolean approve, @Size(max = 500) String note) { }

    public record ExpenseResponse(Long id, Long tripId, String tripNumber, Long staffId, String staffName,
                                  ExpenseCategory category, BigDecimal amount, String currency, String description,
                                  ExpenseStatus status, String reviewNote, LocalDateTime reviewedAt,
                                  LocalDateTime createdAt) { }
}
