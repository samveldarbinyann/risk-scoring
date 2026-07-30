package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.dto.AlertView;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<AlertView> listAlerts(@AuthenticationPrincipal AuthenticatedUser user) {
        return alertService.listAlerts(user.id());
    }
}
