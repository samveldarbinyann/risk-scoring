package com.riskscoring.gateway.controller;

import com.riskscoring.common.event.ScanSource;
import com.riskscoring.gateway.dto.RecentScanGroupView;
import com.riskscoring.gateway.dto.ScanCreateRequest;
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.dto.ScanGroupReportView;
import com.riskscoring.gateway.dto.ScanGroupView;
import com.riskscoring.gateway.dto.ScanHistoryPageView;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.ScanService;
import com.riskscoring.gateway.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scans")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ScanGroupAcceptedResponse requestScan(@Valid @RequestBody ScanCreateRequest request,
                                                 HttpServletRequest httpRequest,
                                                 @AuthenticationPrincipal AuthenticatedUser user) {
        UUID userId = user == null ? null : user.id();
        return scanService.requestScan(ClientIpResolver.resolve(httpRequest), userId, request);
    }

    @GetMapping("/recent")
    public List<RecentScanGroupView> getRecentScans(@AuthenticationPrincipal AuthenticatedUser user) {
        return scanService.getRecentScans(user.id());
    }

    @GetMapping
    public ScanHistoryPageView getScanHistory(@AuthenticationPrincipal AuthenticatedUser user,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(required = false) ScanSource source) {
        return scanService.getScanHistory(user.id(), source, page, size);
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