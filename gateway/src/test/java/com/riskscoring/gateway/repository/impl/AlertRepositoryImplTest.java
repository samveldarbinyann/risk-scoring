package com.riskscoring.gateway.repository.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.repository.AlertRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AlertRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ResultSet resultSet;

    private AlertRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new AlertRepositoryImpl(jdbcTemplate);
    }

    @Test
    void findAllByUserIdMapsEveryColumnFromTheResultSet() throws SQLException {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        UUID watchlistEntryId = UUID.randomUUID();
        UUID scanId = UUID.randomUUID();
        Instant triggeredAt = Instant.parse("2024-06-01T00:00:00Z");
        when(resultSet.getString("id")).thenReturn(id.toString());
        when(resultSet.getString("watchlist_entry_id")).thenReturn(watchlistEntryId.toString());
        when(resultSet.getString("address")).thenReturn("0xabc");
        when(resultSet.getString("chain")).thenReturn("ETHEREUM");
        when(resultSet.getString("previous_risk_level")).thenReturn("LOW");
        when(resultSet.getInt("previous_score")).thenReturn(10);
        when(resultSet.getString("new_risk_level")).thenReturn("HIGH");
        when(resultSet.getInt("new_score")).thenReturn(70);
        when(resultSet.getString("scan_id")).thenReturn(scanId.toString());
        when(resultSet.getTimestamp("triggered_at")).thenReturn(Timestamp.from(triggeredAt));

        ArgumentCaptor<RowMapper<AlertRow>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), captor.capture(), eq(userId))).thenReturn(List.of());

        repository.findAllByUserId(userId);

        AlertRow row = captor.getValue().mapRow(resultSet, 1);
        assertThat(row.id()).isEqualTo(id);
        assertThat(row.watchlistEntryId()).isEqualTo(watchlistEntryId);
        assertThat(row.address()).isEqualTo("0xabc");
        assertThat(row.chain()).isEqualTo(Chain.ETHEREUM);
        assertThat(row.previousRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(row.previousScore()).isEqualTo(10);
        assertThat(row.newRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(row.newScore()).isEqualTo(70);
        assertThat(row.scanId()).isEqualTo(scanId);
        assertThat(row.triggeredAt()).isEqualTo(triggeredAt);
    }

    @Test
    void findAllByUserIdReturnsEmptyListWhenNoRowsMatch() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(UUID.class))).thenReturn(List.of());

        assertThat(repository.findAllByUserId(UUID.randomUUID())).isEmpty();
    }
}
