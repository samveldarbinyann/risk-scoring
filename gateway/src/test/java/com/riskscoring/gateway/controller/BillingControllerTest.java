package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.controller.support.GatewayControllerTest;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.exception.SubscriptionNotFoundException;
import com.riskscoring.gateway.model.PlanCode;
import com.riskscoring.gateway.model.SubscriptionStatus;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.BillingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@GatewayControllerTest(BillingController.class)
class BillingControllerTest extends AbstractControllerTest {

    @MockitoBean
    private BillingService billingService;

    private final AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "alice", UserRole.USER);

    @Test
    void listPlansIsPublic() throws Exception {
        given(billingService.listPlans()).willReturn(
                List.of(new PlanView(PlanCode.FREE, 0, "USD", 10)));

        mockMvc.perform(get("/api/billing/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("FREE"))
                .andExpect(jsonPath("$[0].monthlyRequestLimit").value(10));
    }

    @Test
    void getSubscriptionReturnsCurrentUserSubscription() throws Exception {
        given(billingService.getSubscription(user.id())).willReturn(subscription());

        mockMvc.perform(get("/api/billing/subscription").with(authenticatedAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("STARTER"));
    }

    @Test
    void getSubscriptionReturnsNotFoundWhenMissing() throws Exception {
        given(billingService.getSubscription(user.id())).willThrow(new SubscriptionNotFoundException());

        mockMvc.perform(get("/api/billing/subscription").with(authenticatedAs(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SUBSCRIPTION_NOT_FOUND"));
    }

    @Test
    void activateCreatesSubscription() throws Exception {
        given(billingService.activate(user.id(), PlanCode.STARTER)).willReturn(subscription());

        mockMvc.perform(post("/api/billing/subscription").with(authenticatedAs(user))
                        .contentType("application/json")
                        .content("""
                                {"planCode": "STARTER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planCode").value("STARTER"));
    }

    @Test
    void confirmPaymentActivatesPendingSubscription() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        given(billingService.confirmPayment(user.id(), subscriptionId)).willReturn(subscription());

        mockMvc.perform(post("/api/billing/subscription/{id}/confirm", subscriptionId).with(authenticatedAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void cancelCancelsCurrentSubscription() throws Exception {
        given(billingService.cancel(user.id())).willReturn(subscription());

        mockMvc.perform(post("/api/billing/subscription/cancel").with(authenticatedAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("STARTER"));
    }

    private static SubscriptionView subscription() {
        return new SubscriptionView(
                UUID.randomUUID(), PlanCode.STARTER, SubscriptionStatus.ACTIVE, 2000, "USD", 1000, 10, 990,
                Instant.now(), Instant.now().plusSeconds(2_592_000), Instant.now(), null);
    }
}
