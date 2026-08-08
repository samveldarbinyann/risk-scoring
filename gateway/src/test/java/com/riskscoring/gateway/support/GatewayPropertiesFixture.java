package com.riskscoring.gateway.support;

import com.riskscoring.common.model.Chain;
import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.model.PlanCode;

import java.time.Duration;
import java.util.List;

public final class GatewayPropertiesFixture {

    public static final GatewayProperties.Plan FREE_PLAN =
            new GatewayProperties.Plan(PlanCode.FREE, 0, "USD", 10);
    public static final GatewayProperties.Plan STARTER_PLAN =
            new GatewayProperties.Plan(PlanCode.STARTER, 2_000, "USD", 1_000);
    public static final GatewayProperties.Plan GROWTH_PLAN =
            new GatewayProperties.Plan(PlanCode.GROWTH, 6_000, "USD", 5_000);
    public static final GatewayProperties.Plan SCALE_PLAN =
            new GatewayProperties.Plan(PlanCode.SCALE, 10_000, "USD", 15_000);

    private static final GatewayProperties.Cors CORS =
            new GatewayProperties.Cors(List.of("http://localhost:5173"));
    private static final GatewayProperties.Auth AUTH = new GatewayProperties.Auth(
            "12345678901234567890123456789012", Duration.ofMinutes(15), Duration.ofDays(30), 5,
            Duration.ofMinutes(15), false);
    private static final GatewayProperties.Verification VERIFICATION = new GatewayProperties.Verification(
            "1234567890123456", Duration.ofMinutes(10), Duration.ofSeconds(60), 5);
    private static final GatewayProperties.ApiKeys API_KEYS = new GatewayProperties.ApiKeys(
            "1234567890123456", "rsk_", 5, Duration.ofMinutes(5));
    private static final GatewayProperties.Payment PAYMENT = new GatewayProperties.Payment(
            "0xTestPaymentAddress", Chain.BNB_SMART_CHAIN, "0xTestUsdtContract", 18,
            Duration.ofMinutes(45), 1, 9999, Duration.ofMinutes(5));

    private GatewayPropertiesFixture() {
    }

    public static GatewayProperties gatewayProperties() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String mailFrom = "test@example.com";
        private String contactRecipient = "contact@example.com";
        private Duration billingPeriod = Duration.ofDays(30);
        private List<GatewayProperties.Plan> plans = List.of(FREE_PLAN);
        private GatewayProperties.Payment payment = PAYMENT;
        private int maxChains = 1;
        private GatewayProperties.RateLimit publicScanLimit = new GatewayProperties.RateLimit(10, Duration.ofHours(1));
        private GatewayProperties.RateLimit contactLimit = new GatewayProperties.RateLimit(5, Duration.ofHours(1));
        private GatewayProperties.RateLimit passwordResetLimit =
                new GatewayProperties.RateLimit(5, Duration.ofHours(1));

        public Builder mailFrom(String from) {
            this.mailFrom = from;
            return this;
        }

        public Builder contactRecipient(String recipient) {
            this.contactRecipient = recipient;
            return this;
        }

        public Builder billingPeriod(Duration period) {
            this.billingPeriod = period;
            return this;
        }

        public Builder plans(GatewayProperties.Plan... plans) {
            this.plans = List.of(plans);
            return this;
        }

        public Builder payment(GatewayProperties.Payment payment) {
            this.payment = payment;
            return this;
        }

        public Builder maxChains(int maxChains) {
            this.maxChains = maxChains;
            return this;
        }

        public Builder publicScanLimit(GatewayProperties.RateLimit limit) {
            this.publicScanLimit = limit;
            return this;
        }

        public Builder contactLimit(GatewayProperties.RateLimit limit) {
            this.contactLimit = limit;
            return this;
        }

        public Builder passwordResetLimit(GatewayProperties.RateLimit limit) {
            this.passwordResetLimit = limit;
            return this;
        }

        public GatewayProperties build() {
            return new GatewayProperties(
                    CORS,
                    AUTH,
                    new GatewayProperties.Mail(mailFrom, contactRecipient),
                    VERIFICATION,
                    new GatewayProperties.Billing(billingPeriod, plans, payment),
                    API_KEYS,
                    new GatewayProperties.PublicScan(publicScanLimit, maxChains),
                    new GatewayProperties.Contact(contactLimit),
                    new GatewayProperties.PasswordReset(passwordResetLimit)
            );
        }
    }
}
