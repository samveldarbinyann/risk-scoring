package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.dto.ActivateSubscriptionRequest;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/plans")
    public List<PlanView> listPlans() {
        return billingService.listPlans();
    }

    @GetMapping("/subscription")
    public SubscriptionView getSubscription(@AuthenticationPrincipal AuthenticatedUser user) {
        return billingService.getSubscription(user.id());
    }

    @PostMapping("/subscription")
    public SubscriptionView activate(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody ActivateSubscriptionRequest request) {
        return billingService.activate(user.id(), request.planCode());
    }

    @PostMapping("/subscription/cancel")
    public SubscriptionView cancel(@AuthenticationPrincipal AuthenticatedUser user) {
        return billingService.cancel(user.id());
    }
}
