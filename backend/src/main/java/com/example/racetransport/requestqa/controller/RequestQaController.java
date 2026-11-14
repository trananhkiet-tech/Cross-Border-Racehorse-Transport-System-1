package com.example.racetransport.requestqa.controller;

import com.example.racetransport.requestqa.dto.QaDtos.AnswerRequest;
import com.example.racetransport.requestqa.dto.QaDtos.AskRequest;
import com.example.racetransport.requestqa.dto.QaDtos.QuestionResponse;
import com.example.racetransport.requestqa.service.RequestQaService;
import com.example.racetransport.security.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Request Q&A")
@PreAuthorize("hasAnyRole('CUSTOMER','LOGISTICS_MANAGER','TRANSPORT_SPECIALIST')")
public class RequestQaController {

    private final RequestQaService service;

    public RequestQaController(RequestQaService service) {
        this.service = service;
    }

    @GetMapping("/requests/{id}/questions")
    public List<QuestionResponse> list(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id) {
        return service.listByRequest(actor, id);
    }

    @PostMapping("/requests/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse ask(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                @Valid @RequestBody AskRequest req) {
        return service.ask(actor, id, req);
    }

    @PostMapping("/questions/{id}/answer")
    public QuestionResponse answer(@AuthenticationPrincipal UserPrincipal actor, @PathVariable Long id,
                                   @Valid @RequestBody AnswerRequest req) {
        return service.answer(actor, id, req);
    }

    @GetMapping("/questions/inbox")
    public List<QuestionResponse> inbox(@AuthenticationPrincipal UserPrincipal actor) {
        return service.inbox(actor);
    }
}
