package com.riskscoring.monitor.mapper;

import com.riskscoring.common.event.AlertTriggered;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.monitor.entity.Alert;
import com.riskscoring.monitor.entity.WatchlistEntry;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AlertMapper {

    public Alert toEntity(WatchlistEntry entry, RiskLevel newLevel, int newScore, UUID scanId, Instant triggeredAt) {
        return Alert.builder()
                .id(UUID.randomUUID())
                .watchlistEntryId(entry.getId())
                .userId(entry.getUserId())
                .address(entry.getAddress())
                .chain(entry.getChain())
                .previousRiskLevel(entry.getLastRiskLevel())
                .previousScore(entry.getLastScore())
                .newRiskLevel(newLevel)
                .newScore(newScore)
                .scanId(scanId)
                .triggeredAt(triggeredAt)
                .build();
    }

    public AlertTriggered toEvent(Alert alert) {
        return new AlertTriggered(
                alert.getId(),
                alert.getWatchlistEntryId(),
                alert.getUserId(),
                alert.getAddress(),
                alert.getChain(),
                alert.getPreviousRiskLevel(),
                alert.getPreviousScore(),
                alert.getNewRiskLevel(),
                alert.getNewScore(),
                alert.getScanId(),
                alert.getTriggeredAt()
        );
    }
}