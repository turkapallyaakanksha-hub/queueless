package com.queueless.controller;

import com.queueless.dto.Dtos.TakeTokenRequest;
import com.queueless.dto.Dtos.TokenView;
import com.queueless.model.User;
import com.queueless.service.QueueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tokens")
public class TokenController {

    private final QueueService queueService;

    public TokenController(QueueService queueService) {
        this.queueService = queueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TokenView take(@AuthenticationPrincipal User user, @Valid @RequestBody TakeTokenRequest request) {
        return queueService.takeToken(user, request.serviceId());
    }

    @GetMapping("/my")
    public List<TokenView> mine(@AuthenticationPrincipal User user) {
        return queueService.myTokens(user);
    }

    @PostMapping("/{id}/cancel")
    public TokenView cancel(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return queueService.cancelMyToken(user, id);
    }
}
