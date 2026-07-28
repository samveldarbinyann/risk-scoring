package com.riskscoring.gateway.repository.impl;

import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.repository.ScanReportRepository;
import com.riskscoring.gateway.repository.ScanReportRow;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
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
            SELECT scan_id, address, chain_id, risk_level, score, explanation,
                   decisive_signals, manual_checks, model, created_at
            FROM riskai.scan_report
            WHERE scan_id = ?
            """;

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<ScanReportRow> findByScanId(UUID scanId) {
        return jdbcTemplate.query(FIND_BY_SCAN_ID, (rs, rowNum) -> new ScanReportRow(
                UUID.fromString(rs.getString("scan_id")),
                rs.getString("address"),
                rs.getInt("chain_id"),
                RiskLevel.valueOf(rs.getString("risk_level")),
                rs.getInt("score"),
                rs.getString("explanation"),
                objectMapper.readValue(rs.getString("decisive_signals"), STRING_LIST),
                objectMapper.readValue(rs.getString("manual_checks"), STRING_LIST),
                rs.getString("model"),
                rs.getTimestamp("created_at").toInstant()
        ), scanId).stream().findFirst();
    }
}