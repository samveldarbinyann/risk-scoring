package com.riskscoring.gateway.repository.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.repository.WatchlistEntryRow;
import com.riskscoring.gateway.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WatchlistRepositoryImpl implements WatchlistRepository {

    private static final String FIND_ALL_BY_USER_ID = """
            SELECT id, address, chain, last_risk_level, last_score,
                   last_scan_id, last_checked_at, created_at
            FROM monitor.watchlist_entry
            WHERE user_id = ? AND active = true
            ORDER BY created_at DESC
            """;

    private static final String EXISTS_BY_ID_AND_USER_ID = """
            SELECT EXISTS (SELECT 1 FROM monitor.watchlist_entry WHERE id = ? AND user_id = ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<WatchlistEntryRow> findAllByUserId(UUID userId) {
        return jdbcTemplate.query(FIND_ALL_BY_USER_ID, (rs, rowNum) -> {
            String riskLevel = rs.getString("last_risk_level");
            String lastScanId = rs.getString("last_scan_id");
            Timestamp lastCheckedAt = rs.getTimestamp("last_checked_at");

            return new WatchlistEntryRow(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("address"),
                    Chain.valueOf(rs.getString("chain")),
                    riskLevel != null ? RiskLevel.valueOf(riskLevel) : null,
                    rs.getObject("last_score", Integer.class),
                    lastScanId != null ? UUID.fromString(lastScanId) : null,
                    lastCheckedAt != null ? lastCheckedAt.toInstant() : null,
                    rs.getTimestamp("created_at").toInstant()
            );
        }, userId);
    }

    @Override
    public boolean existsByIdAndUserId(UUID id, UUID userId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(EXISTS_BY_ID_AND_USER_ID, Boolean.class, id, userId));
    }
}