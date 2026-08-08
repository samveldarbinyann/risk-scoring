package com.riskscoring.gateway.service;

import com.riskscoring.common.event.UsdtPaymentDetected;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.model.PlanCode;

import java.util.List;
import java.util.UUID;

public interface BillingService {

    List<PlanView> listPlans();

    SubscriptionView getSubscription(UUID userId);

    SubscriptionView activate(UUID userId, PlanCode planCode);

    void confirmPaymentFromChain(UsdtPaymentDetected event);

    void expireOverduePayments();

    SubscriptionView cancel(UUID userId);

    void requireActiveSubscription(UUID userId);

    void chargeQuota(UUID userId, int units);
}
