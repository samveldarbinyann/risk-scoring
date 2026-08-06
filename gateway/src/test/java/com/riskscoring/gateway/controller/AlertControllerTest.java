package com.riskscoring.gateway.controller;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.controller.support.GatewayControllerTest;
import com.riskscoring.gateway.dto.AlertView;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@GatewayControllerTest(AlertController.class)
class AlertControllerTest extends AbstractControllerTest {

    @MockitoBean
    private AlertService alertService;

    @Test
    void listAlertsReturnsAlertsForCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "alice", UserRole.USER);
        AlertView alert = new AlertView(
                UUID.randomUUID(), UUID.randomUUID(), "0xabc", Chain.ETHEREUM,
                RiskLevel.LOW, 10, RiskLevel.HIGH, 80, UUID.randomUUID(), Instant.now());
        given(alertService.listAlerts(userId)).willReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts").with(authenticatedAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].address").value("0xabc"))
                .andExpect(jsonPath("$[0].newRiskLevel").value("HIGH"));

        verify(alertService).listAlerts(userId);
    }
}
