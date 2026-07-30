package com.riskscoring.gateway.repository.impl;

import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.repository.WatchlistEntryRow;
import com.riskscoring.gateway.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WatchlistRepositoryImpl implements WatchlistRepository {

    private static final String COLUMNS = """
            id, address, chain_id, active, last_risk_level, last_score, last_scan_id, last_checked_at, created_at
            """;

    private static final String FIND_ALL_BY_USER_ID = """
            SELECT %s
            FROM monitor.watchlist_entry
            WHERE user_id = ? AND active = true
            ORDER BY created_at DESC
            """.formatted(COLUMNS);

    private static final String FIND_BY_ID_AND_USER_ID = """
            SELECT %s
            FROM monitor.watchlist_entry
            WHERE id = ? AND user_id = ?
            """.formatted(COLUMNS);

    private static final RowMapper<WatchlistEntryRow> ROW_MAPPER = (rs, rowNum) -> {
        String riskLevel = rs.getString("last_risk_level");
        int lastScore = rs.getInt("last_score");
        boolean lastScoreNull = rs.wasNull();
        String lastScanId = rs.getString("last_scan_id");
        Timestamp lastCheckedAt = rs.getTimestamp("last_checked_at");

        return new WatchlistEntryRow(
                UUID.fromString(rs.getString("id")),
                rs.getString("address"),
                rs.getInt("chain_id"),
                rs.getBoolean("active"),
                riskLevel != null ? RiskLevel.valueOf(riskLevel) : null,
                lastScoreNull ? null : lastScore,
                lastScanId != null ? UUID.fromString(lastScanId) : null,
                lastCheckedAt != null ? lastCheckedAt.toInstant() : null,
                rs.getTimestamp("created_at").toInstant()
        );
    };

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<WatchlistEntryRow> findAllByUserId(UUID userId) {
        return jdbcTemplate.query(FIND_ALL_BY_USER_ID, ROW_MAPPER, userId);
    }

    @Override
    public Optional<WatchlistEntryRow> findByIdAndUserId(UUID id, UUID userId) {
        return jdbcTemplate.query(FIND_BY_ID_AND_USER_ID, ROW_MAPPER, id, userId).stream().findFirst();
    }
}
