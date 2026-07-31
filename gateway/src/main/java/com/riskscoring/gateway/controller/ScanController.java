package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.dto.ScanCreateRequest;
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.dto.ScanGroupReportView;
import com.riskscoring.gateway.dto.ScanGroupView;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.service.ScanService;
import com.riskscoring.gateway.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
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
    public ScanGroupAcceptedResponse requestScan(@Valid @RequestBody ScanCreateRequest request,
                                                 HttpServletRequest httpRequest) {
        return scanService.requestScan(ClientIpResolver.resolve(httpRequest), request);
    }

    @GetMapping("/groups/{groupId}")
    public ScanGroupView getScanGroup(@PathVariable UUID groupId) {
        return scanService.getScanGroup(groupId);
    }

    @GetMapping("/groups/{groupId}/report")
    public ScanGroupReportView getScanGroupReport(@PathVariable UUID groupId) {
        return scanService.getScanGroupReport(groupId);
    }

    @GetMapping("/{scanId}")
    public ScanView getScan(@PathVariable UUID scanId) {
        return scanService.getScan(scanId);
    }

    @GetMapping("/{scanId}/report")
    public ScanReportView getScanReport(@PathVariable UUID scanId) {
        return scanService.getScanReport(scanId);
    }
}