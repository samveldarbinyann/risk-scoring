package com.riskscoring.gateway.repository.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.repository.ScanReportRepository;
import com.riskscoring.gateway.repository.ScanReportRow;
import com.riskscoring.gateway.repository.ScanRiskSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ScanReportRepositoryImpl implements ScanReportRepository {

    private static final String FIND_BY_SCAN_ID = """
            SELECT scan_id, target_type, target, chain, risk_level, score, explanation,
                   decisive_signals, manual_checks, observed_at, evidence, model, created_at
            FROM riskai.scan_report
            WHERE scan_id = ?
            """;

    private static final String FIND_RISK_SUMMARIES = """
            SELECT scan_id, risk_level, score
            FROM riskai.scan_report
            WHERE scan_id IN (:scanIds)
            """;

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<ScanReportRow> findByScanId(UUID scanId) {
        return jdbcTemplate.query(FIND_BY_SCAN_ID, (rs, rowNum) -> new ScanReportRow(
                UUID.fromString(rs.getString("scan_id")),
                ScanTarget.valueOf(rs.getString("target_type")),
                rs.getString("target"),
                Chain.valueOf(rs.getString("chain")),
                RiskLevel.valueOf(rs.getString("risk_level")),
                rs.getInt("score"),
                rs.getString("explanation"),
                objectMapper.readValue(rs.getString("decisive_signals"), STRING_LIST),
                objectMapper.readValue(rs.getString("manual_checks"), STRING_LIST),
                rs.getTimestamp("observed_at").toInstant(),
                objectMapper.readValue(rs.getString("evidence"), EvidenceBundle.class),
                rs.getString("model"),
                rs.getTimestamp("created_at").toInstant()
        ), scanId).stream().findFirst();
    }

    @Override
    public List<ScanRiskSummary> findRiskSummaries(List<UUID> scanIds) {
        if (scanIds.isEmpty()) {
            return List.of();
        }

        var params = new MapSqlParameterSource("scanIds", scanIds);
        return namedParameterJdbcTemplate.query(FIND_RISK_SUMMARIES, params, (rs, rowNum) -> new ScanRiskSummary(
                UUID.fromString(rs.getString("scan_id")),
                RiskLevel.valueOf(rs.getString("risk_level")),
                rs.getInt("score")
        ));
    }
}
