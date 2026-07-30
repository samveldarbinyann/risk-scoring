package com.riskscoring.gateway.repository.impl;

import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.repository.AlertRepository;
import com.riskscoring.gateway.repository.AlertRow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AlertRepositoryImpl implements AlertRepository {

    private static final String FIND_ALL_BY_USER_ID = """
            SELECT id, watchlist_entry_id, address, chain_id, previous_risk_level, previous_score,
                   new_risk_level, new_score, scan_id, triggered_at
            FROM monitor.alert
            WHERE user_id = ?
            ORDER BY triggered_at DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<AlertRow> findAllByUserId(UUID userId) {
        return jdbcTemplate.query(FIND_ALL_BY_USER_ID, (rs, rowNum) -> new AlertRow(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("watchlist_entry_id")),
                rs.getString("address"),
                rs.getInt("chain_id"),
                RiskLevel.valueOf(rs.getString("previous_risk_level")),
                rs.getInt("previous_score"),
                RiskLevel.valueOf(rs.getString("new_risk_level")),
                rs.getInt("new_score"),
                UUID.fromString(rs.getString("scan_id")),
                rs.getTimestamp("triggered_at").toInstant()
        ), userId);
    }
}
