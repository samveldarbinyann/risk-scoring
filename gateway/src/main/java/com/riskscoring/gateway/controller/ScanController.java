package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.dto.ScanAcceptedResponse;
import com.riskscoring.gateway.dto.ScanCreateRequest;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.service.ScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/scans")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ScanAcceptedResponse requestScan(@Valid @RequestBody ScanCreateRequest request) {
        return scanService.requestScan(request);
    }

    @GetMapping("/{scanId}")
    public ScanView getScan(@PathVariable UUID scanId) {
        return scanService.getScan(scanId);
    }
}