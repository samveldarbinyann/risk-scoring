package com.riskscoring.gateway.mapper;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.dto.AlertView;
import com.riskscoring.gateway.repository.AlertRow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AlertMapperTest {

    private final AlertMapper mapper = new AlertMapper();

    @Test
    void toViewMapsAllFieldsFromRow() {
        AlertRow row = new AlertRow(UUID.randomUUID(), UUID.randomUUID(), "0xabc", Chain.ETHEREUM,
                RiskLevel.LOW, 10, RiskLevel.HIGH, 70, UUID.randomUUID(), Instant.now());

        AlertView view = mapper.toView(row);

        assertThat(view).isEqualTo(new AlertView(row.id(), row.watchlistEntryId(), row.address(), row.chain(),
                row.previousRiskLevel(), row.previousScore(), row.newRiskLevel(), row.newScore(),
                row.scanId(), row.triggeredAt()));
    }
}
