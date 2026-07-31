package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.dto.ScanCreateRequest;
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.security.ApiKeyPrincipal;
import com.riskscoring.gateway.service.ScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
public class ApiV1ScanController {

    private final ScanService scanService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ScanGroupAcceptedResponse requestScan(@AuthenticationPrincipal ApiKeyPrincipal principal,
                                                 @Valid @RequestBody ScanCreateRequest request) {
        return scanService.requestApiScan(principal.userId(), request);
    }
}
