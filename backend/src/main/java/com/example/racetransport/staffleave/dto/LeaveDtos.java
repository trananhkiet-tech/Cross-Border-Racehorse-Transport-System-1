package com.example.racetransport.staffleave.dto;

import com.example.racetransport.staffleave.entity.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class LeaveDtos {

    private LeaveDtos() { }

    public record LeaveRequest(@NotNull Long staffId, @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                               @Size(max = 300) String reason) { }

    public record LeaveResponse(Long id, Long staffId, String staffName, LocalDate startDate, LocalDate endDate,
                                String reason, LeaveStatus status) { }
}
