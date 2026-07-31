package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.dto.ActivateSubscriptionRequest;
import com.riskscoring.gateway.dto.PlanView;
import com.riskscoring.gateway.dto.SubscriptionView;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionView activate(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody ActivateSubscriptionRequest request) {
        return billingService.activate(user.id(), request.planCode());
    }

    @PostMapping("/subscription/{id}/confirm")
    public SubscriptionView confirmPayment(@PathVariable UUID id) {
        return billingService.confirmPayment(id);
    }

    @PostMapping("/subscription/cancel")
    public SubscriptionView cancel(@AuthenticationPrincipal AuthenticatedUser user) {
        return billingService.cancel(user.id());
    }
}
