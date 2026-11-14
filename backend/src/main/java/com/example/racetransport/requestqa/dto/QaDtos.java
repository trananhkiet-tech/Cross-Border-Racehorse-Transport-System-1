package com.example.racetransport.requestqa.dto;

import com.example.racetransport.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class QaDtos {

    private QaDtos() { }

    public record AskRequest(@NotBlank @Size(max = 1000) String question) { }

    public record AnswerRequest(@NotBlank @Size(max = 2000) String answer) { }

    public record QuestionResponse(Long id, Long requestId, String requestNumber, String question, String askedByName,
                                   Role askedByRole, LocalDateTime createdAt, String answer, String answeredByName,
                                   LocalDateTime answeredAt, boolean canAnswer) { }
}
