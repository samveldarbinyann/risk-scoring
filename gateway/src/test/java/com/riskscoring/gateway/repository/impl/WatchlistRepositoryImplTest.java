package com.riskscoring.gateway.repository.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.repository.WatchlistEntryRow;
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
class WatchlistRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ResultSet resultSet;

    private WatchlistRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new WatchlistRepositoryImpl(jdbcTemplate);
    }

    @Test
    void findAllByUserIdMapsRowWithAllNullableFieldsPopulated() throws SQLException {
        UUID id = UUID.randomUUID();
        UUID lastScanId = UUID.randomUUID();
        Instant lastCheckedAt = Instant.parse("2024-06-01T00:00:00Z");
        Instant createdAt = Instant.parse("2024-05-01T00:00:00Z");
        when(resultSet.getString("id")).thenReturn(id.toString());
        when(resultSet.getString("address")).thenReturn("0xabc");
        when(resultSet.getString("chain")).thenReturn("ETHEREUM");
        when(resultSet.getString("last_risk_level")).thenReturn("LOW");
        when(resultSet.getObject("last_score", Integer.class)).thenReturn(10);
        when(resultSet.getString("last_scan_id")).thenReturn(lastScanId.toString());
        when(resultSet.getTimestamp("last_checked_at")).thenReturn(Timestamp.from(lastCheckedAt));
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(createdAt));

        ArgumentCaptor<RowMapper<WatchlistEntryRow>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), captor.capture(), any(UUID.class))).thenReturn(List.of());

        repository.findAllByUserId(UUID.randomUUID());

        WatchlistEntryRow row = captor.getValue().mapRow(resultSet, 1);
        assertThat(row.id()).isEqualTo(id);
        assertThat(row.address()).isEqualTo("0xabc");
        assertThat(row.chain()).isEqualTo(Chain.ETHEREUM);
        assertThat(row.lastRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(row.lastScore()).isEqualTo(10);
        assertThat(row.lastScanId()).isEqualTo(lastScanId);
        assertThat(row.lastCheckedAt()).isEqualTo(lastCheckedAt);
        assertThat(row.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void findAllByUserIdMapsNullableFieldsAsNullForNeverCheckedEntry() throws SQLException {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-05-01T00:00:00Z");
        when(resultSet.getString("id")).thenReturn(id.toString());
        when(resultSet.getString("address")).thenReturn("0xabc");
        when(resultSet.getString("chain")).thenReturn("ETHEREUM");
        when(resultSet.getString("last_risk_level")).thenReturn(null);
        when(resultSet.getObject("last_score", Integer.class)).thenReturn(null);
        when(resultSet.getString("last_scan_id")).thenReturn(null);
        when(resultSet.getTimestamp("last_checked_at")).thenReturn(null);
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.from(createdAt));

        ArgumentCaptor<RowMapper<WatchlistEntryRow>> captor = ArgumentCaptor.forClass(RowMapper.class);
        when(jdbcTemplate.query(anyString(), captor.capture(), any(UUID.class))).thenReturn(List.of());

        repository.findAllByUserId(UUID.randomUUID());

        WatchlistEntryRow row = captor.getValue().mapRow(resultSet, 1);
        assertThat(row.lastRiskLevel()).isNull();
        assertThat(row.lastScore()).isNull();
        assertThat(row.lastScanId()).isNull();
        assertThat(row.lastCheckedAt()).isNull();
        assertThat(row.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void existsByIdAndUserIdReturnsTrueWhenQueryReturnsTrue() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(UUID.class), any(UUID.class)))
                .thenReturn(Boolean.TRUE);

        assertThat(repository.existsByIdAndUserId(UUID.randomUUID(), UUID.randomUUID())).isTrue();
    }

    @Test
    void existsByIdAndUserIdReturnsFalseWhenQueryReturnsFalse() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(UUID.class), any(UUID.class)))
                .thenReturn(Boolean.FALSE);

        assertThat(repository.existsByIdAndUserId(UUID.randomUUID(), UUID.randomUUID())).isFalse();
    }

    @Test
    void existsByIdAndUserIdReturnsFalseWhenQueryReturnsNull() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any(UUID.class), any(UUID.class)))
                .thenReturn(null);

        assertThat(repository.existsByIdAndUserId(UUID.randomUUID(), UUID.randomUUID())).isFalse();
    }
}
