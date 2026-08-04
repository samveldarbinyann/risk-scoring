package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.helius.HeliusNativeTransfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusTokenTransfer;
import com.riskscoring.chainingest.client.dto.helius.HeliusTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class SolanaValues implements ChainAddressValues {

    private static final String SYSTEM_PROGRAM = "11111111111111111111111111111111";
    private static final long NO_TIMESTAMP = 0L;

    public String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String address(String value) {
        return normalize(value);
    }

    @Override
    public boolean isRoutable(String address) {
        return !address.isEmpty() && !SYSTEM_PROGRAM.equals(address);
    }

    public Instant timestamp(long epochSeconds) {
        return epochSeconds == NO_TIMESTAMP ? null : Instant.ofEpochSecond(epochSeconds);
    }

    public BigDecimal amount(BigDecimal value) {
        return Optional.ofNullable(value).orElse(BigDecimal.ZERO);
    }

    public boolean succeeded(HeliusTransaction transaction) {
        return transaction.transactionError() == null;
    }

    public List<HeliusNativeTransfer> nativeTransfers(HeliusTransaction transaction) {
        return Objects.requireNonNullElse(transaction.nativeTransfers(), List.of());
    }

    public List<HeliusTokenTransfer> tokenTransfers(HeliusTransaction transaction) {
        return Objects.requireNonNullElse(transaction.tokenTransfers(), List.of());
    }
}
