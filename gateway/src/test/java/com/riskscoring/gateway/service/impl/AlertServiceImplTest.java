package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.dto.AlertView;
import com.riskscoring.gateway.mapper.AlertMapper;
import com.riskscoring.gateway.repository.AlertRepository;
import com.riskscoring.gateway.repository.AlertRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    private AlertServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlertServiceImpl(alertRepository, new AlertMapper());
    }

    @Test
    void listAlertsMapsEachRowToAView() {
        UUID userId = UUID.randomUUID();
        AlertRow row = new AlertRow(UUID.randomUUID(), UUID.randomUUID(), "0xabc", Chain.ETHEREUM,
                RiskLevel.LOW, 10, RiskLevel.HIGH, 70, UUID.randomUUID(), Instant.now());
        when(alertRepository.findAllByUserId(userId)).thenReturn(List.of(row));

        List<AlertView> views = service.listAlerts(userId);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().id()).isEqualTo(row.id());
    }

    @Test
    void listAlertsReturnsEmptyListWhenUserHasNoAlerts() {
        UUID userId = UUID.randomUUID();
        when(alertRepository.findAllByUserId(userId)).thenReturn(List.of());

        assertThat(service.listAlerts(userId)).isEmpty();
    }
}
