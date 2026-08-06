package com.riskscoring.gateway.controller;

import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.controller.support.GatewayControllerTest;
import com.riskscoring.gateway.dto.RecentScanGroupView;
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.dto.ScanGroupChainStatus;
import com.riskscoring.gateway.dto.ScanGroupReportView;
import com.riskscoring.gateway.dto.ScanGroupView;
import com.riskscoring.gateway.dto.ScanHistoryPageView;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.exception.ScanGroupNotFoundException;
import com.riskscoring.gateway.exception.ScanReportNotReadyException;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.ScanService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@GatewayControllerTest(ScanController.class)
class ScanControllerTest extends AbstractControllerTest {

    @MockitoBean
    private ScanService scanService;

    private final AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "alice", UserRole.USER);

    @Test
    void requestScanByAuthenticatedUser() throws Exception {
        UUID groupId = UUID.randomUUID();
        given(scanService.requestScan(anyString(), eq(user.id()), any())).willReturn(
                new ScanGroupAcceptedResponse(groupId, ScanTarget.ADDRESS, "0xabc", List.of(Chain.ETHEREUM)));

        mockMvc.perform(post("/api/scans").with(authenticatedAs(user))
                        .contentType("application/json")
                        .content("""
                                {"target": "0xabc", "chains": ["ETHEREUM"]}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.groupId").value(groupId.toString()));
    }

    @Test
    void requestScanByAnonymousUser() throws Exception {
        UUID groupId = UUID.randomUUID();
        given(scanService.requestScan(anyString(), isNull(), any())).willReturn(
                new ScanGroupAcceptedResponse(groupId, ScanTarget.ADDRESS, "0xabc", List.of(Chain.ETHEREUM)));

        mockMvc.perform(post("/api/scans")
                        .contentType("application/json")
                        .content("""
                                {"target": "0xabc"}
                                """))
                .andExpect(status().isAccepted());

        verify(scanService).requestScan(anyString(), isNull(), any());
    }

    @Test
    void requestScanRejectsBlankTarget() throws Exception {
        mockMvc.perform(post("/api/scans")
                        .contentType("application/json")
                        .content("""
                                {"target": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void getRecentScansReturnsUserGroups() throws Exception {
        RecentScanGroupView view = new RecentScanGroupView(
                UUID.randomUUID(), ScanTarget.ADDRESS, "0xabc", List.of(Chain.ETHEREUM),
                true, RiskLevel.LOW, 5, Instant.now(), ScanSource.USER);
        given(scanService.getRecentScans(user.id())).willReturn(List.of(view));

        mockMvc.perform(get("/api/scans/recent").with(authenticatedAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].target").value("0xabc"));
    }

    @Test
    void getScanHistoryPassesPagingAndSource() throws Exception {
        given(scanService.getScanHistory(user.id(), ScanSource.USER, 1, 5)).willReturn(
                new ScanHistoryPageView(List.of(), 1, 5, 0, 0, false));

        mockMvc.perform(get("/api/scans").with(authenticatedAs(user))
                        .param("page", "1")
                        .param("size", "5")
                        .param("source", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void getScanGroupReturnsGroupStatus() throws Exception {
        UUID groupId = UUID.randomUUID();
        given(scanService.getScanGroup(groupId)).willReturn(new ScanGroupView(
                groupId, ScanTarget.ADDRESS, "0xabc", true,
                List.of(new ScanGroupChainStatus(Chain.ETHEREUM, UUID.randomUUID(), ScanStage.COMPLETED))));

        mockMvc.perform(get("/api/scans/groups/{groupId}", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void getScanGroupReturnsNotFound() throws Exception {
        UUID groupId = UUID.randomUUID();
        given(scanService.getScanGroup(groupId)).willThrow(new ScanGroupNotFoundException(groupId));

        mockMvc.perform(get("/api/scans/groups/{groupId}", groupId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SCAN_GROUP_NOT_FOUND"));
    }

    @Test
    void getScanGroupReportReturnsReports() throws Exception {
        UUID groupId = UUID.randomUUID();
        given(scanService.getScanGroupReport(groupId)).willReturn(
                new ScanGroupReportView(groupId, ScanTarget.ADDRESS, "0xabc", List.of()));

        mockMvc.perform(get("/api/scans/groups/{groupId}/report", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId.toString()));
    }

    @Test
    void getScanReturnsScan() throws Exception {
        UUID scanId = UUID.randomUUID();
        given(scanService.getScan(scanId)).willReturn(new ScanView(
                scanId, ScanTarget.ADDRESS, "0xabc", Chain.ETHEREUM, ScanStage.COMPLETED,
                ScanSource.USER, Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/scans/{scanId}", scanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getScanReportReturnsReport() throws Exception {
        UUID scanId = UUID.randomUUID();
        given(scanService.getScanReport(scanId)).willReturn(new ScanReportView(
                scanId, ScanTarget.ADDRESS, "0xabc", Chain.ETHEREUM, RiskLevel.HIGH, 80,
                "High risk due to mixer exposure", List.of("mixer_exposure"), List.of(),
                Instant.now(), null, "claude", Instant.now()));

        mockMvc.perform(get("/api/scans/{scanId}/report", scanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.score").value(80));
    }

    @Test
    void getScanReportReturnsConflictWhenNotReady() throws Exception {
        UUID scanId = UUID.randomUUID();
        given(scanService.getScanReport(scanId)).willThrow(new ScanReportNotReadyException(scanId, ScanStage.ANALYZING));

        mockMvc.perform(get("/api/scans/{scanId}/report", scanId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("REPORT_NOT_READY"));
    }
}
