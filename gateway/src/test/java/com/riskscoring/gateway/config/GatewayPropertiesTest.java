package com.riskscoring.gateway.config;

import com.riskscoring.gateway.model.PlanCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayPropertiesTest {

    @Test
    void requirePlanReturnsPlanMatchingCode() {
        GatewayProperties.Billing billing = billing(
                new GatewayProperties.Plan(PlanCode.FREE, 0, "USD", 10),
                new GatewayProperties.Plan(PlanCode.STARTER, 2_000, "USD", 1_000));

        GatewayProperties.Plan plan = billing.requirePlan(PlanCode.STARTER);

        assertThat(plan.code()).isEqualTo(PlanCode.STARTER);
        assertThat(plan.priceCents()).isEqualTo(2_000);
    }

    @Test
    void requirePlanThrowsWhenNoPlanMatchesCode() {
        GatewayProperties.Billing billing = billing(new GatewayProperties.Plan(PlanCode.FREE, 0, "USD", 10));

        assertThatThrownBy(() -> billing.requirePlan(PlanCode.SCALE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No plan configured for SCALE");
    }

    @Test
    void requirePlanThrowsWhenPlanListIsEmpty() {
        GatewayProperties.Billing billing = billing();

        assertThatThrownBy(() -> billing.requirePlan(PlanCode.FREE))
                .isInstanceOf(IllegalStateException.class);
    }

    private static GatewayProperties.Billing billing(GatewayProperties.Plan... plans) {
        GatewayProperties.Payment payment = new GatewayProperties.Payment(
                "0xTestPaymentAddress", "0xTestUsdtContract", Duration.ofMinutes(45), 1, 9999, Duration.ofMinutes(5));
        return new GatewayProperties.Billing(Duration.ofDays(30), List.of(plans), payment);
    }
}
