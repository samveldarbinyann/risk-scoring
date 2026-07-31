package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.dto.ApiKeyCreatedView;
import com.riskscoring.gateway.dto.ApiKeyView;
import com.riskscoring.gateway.dto.CreateApiKeyRequest;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyCreatedView create(@AuthenticationPrincipal AuthenticatedUser user,
                                    @Valid @RequestBody CreateApiKeyRequest request) {
        return apiKeyService.create(user.id(), request);
    }

    @GetMapping
    public List<ApiKeyView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return apiKeyService.list(user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        apiKeyService.revoke(user.id(), id);
    }
}
