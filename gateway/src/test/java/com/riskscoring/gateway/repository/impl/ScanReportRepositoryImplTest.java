package com.riskscoring.gateway.repository.impl;

import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.repository.ScanReportRow;
import com.riskscoring.gateway.repository.ScanRiskSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ScanReportRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private ResultSet resultSet;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ScanReportRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ScanReportRepositoryImpl(jdbcTemplate, namedParameterJdbcTemplate, objectMapper);
    }

    @Test
    void findByScanIdMapsRowIncludingJsonColumns() throws SQLException {
        UUID scanId = UUID.randomUUID();
        Instant observedAt = Instant.parse("2024-06-01T00:00:00Z");
        Instant createdAt = Instant.parse("2024-06-01T00:05:00Z");
        EvidenceBundle evidence = new AddressEvidence("0xabc", Chain.ETHEREUM, observedAt, null, 0, 0, false, "0",
                List.of(), 0, List.of(), null, new Heuristics(null, null, false, 0, 0));
        when(resultSet.getString("scan_id")).thenReturn(scanId.toString());
        when(resultSet.getString("target_type")).thenReturn("ADDRESS");
        when(resultSet.getString("target")).thenReturn("0xabc");
        when(resultSet.getString("chain")).thenReturn("ETHEREUM");
        when(resultSet.getString("risk_level")).thenReturn("LOW");
        when(resultSet.getInt("score")).thenReturn(10);
        when(resultSet.getString("explanation")).thenReturn("clean wallet");
        when(resultSet.getString("decisive_signals")).thenReturn("[\"a\",\"b\"]");
        when(resultSet.getString("manual_checks")).thenReturn("[]");
        when(resultSet.getTimestamp("observed_at")).thenReturn(Timestamp.from(observedAt));
        when(resultSet.getString("evidence")).thenReturn(objectMapper.writeValueAsString(evidence));
        when(resultSet.getString("model")).thenReturn("deepseek-chat");
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(createdAt));

        ArgumentCaptor<RowMapper<ScanReportRow>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), captor.capture(), eq(scanId))).thenReturn(List.of());

        repository.findByScanId(scanId);

        ScanReportRow row = captor.getValue().mapRow(resultSet, 1);
        assertThat(row.scanId()).isEqualTo(scanId);
        assertThat(row.targetType()).isEqualTo(ScanTarget.ADDRESS);
        assertThat(row.chain()).isEqualTo(Chain.ETHEREUM);
        assertThat(row.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(row.score()).isEqualTo(10);
        assertThat(row.decisiveSignals()).containsExactly("a", "b");
        assertThat(row.manualChecks()).isEmpty();
        assertThat(row.observedAt()).isEqualTo(observedAt);
        assertThat(row.evidence()).isEqualTo(evidence);
        assertThat(row.model()).isEqualTo("deepseek-chat");
        assertThat(row.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void findByScanIdReturnsEmptyOptionalWhenNoRowFound() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(UUID.class))).thenReturn(List.of());

        Optional<ScanReportRow> result = repository.findByScanId(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void findRiskSummariesReturnsEmptyListWithoutQueryingWhenInputIsEmpty() {
        List<ScanRiskSummary> result = repository.findRiskSummaries(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(namedParameterJdbcTemplate);
    }

    @Test
    void findRiskSummariesMapsRowsWhenInputIsNonEmpty() throws SQLException {
        UUID scanId = UUID.randomUUID();
        when(resultSet.getString("scan_id")).thenReturn(scanId.toString());
        when(resultSet.getString("risk_level")).thenReturn("HIGH");
        when(resultSet.getInt("score")).thenReturn(70);

        ArgumentCaptor<RowMapper<ScanRiskSummary>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), captor.capture()))
                .thenReturn(List.of());

        repository.findRiskSummaries(List.of(scanId));

        ScanRiskSummary summary = captor.getValue().mapRow(resultSet, 1);
        assertThat(summary.scanId()).isEqualTo(scanId);
        assertThat(summary.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(summary.score()).isEqualTo(70);
    }
}
