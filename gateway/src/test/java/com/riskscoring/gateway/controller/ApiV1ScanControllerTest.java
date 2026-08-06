package com.riskscoring.gateway.controller;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.controller.support.GatewayControllerTest;
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.security.ApiKeyPrincipal;
import com.riskscoring.gateway.service.ScanService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@GatewayControllerTest(ApiV1ScanController.class)
class ApiV1ScanControllerTest extends AbstractControllerTest {

    @MockitoBean
    private ScanService scanService;

    private final ApiKeyPrincipal principal = new ApiKeyPrincipal(UUID.randomUUID(), UUID.randomUUID());

    @Test
    void requestScanReturnsAcceptedGroup() throws Exception {
        UUID groupId = UUID.randomUUID();
        ScanGroupAcceptedResponse response = new ScanGroupAcceptedResponse(
                groupId, ScanTarget.ADDRESS, "0xabc", List.of(Chain.ETHEREUM));
        given(scanService.requestApiScan(eq(principal.userId()), any())).willReturn(response);

        mockMvc.perform(post("/api/v1/scans").with(authenticatedAs(principal))
                        .contentType("application/json")
                        .content("""
                                {"target": "0xabc", "chains": ["ETHEREUM"]}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.groupId").value(groupId.toString()))
                .andExpect(jsonPath("$.target").value("0xabc"));
    }

    @Test
    void requestScanRejectsBlankTarget() throws Exception {
        mockMvc.perform(post("/api/v1/scans").with(authenticatedAs(principal))
                        .contentType("application/json")
                        .content("""
                                {"target": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }
}
