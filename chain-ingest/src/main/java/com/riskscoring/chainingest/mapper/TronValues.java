package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.trongrid.TronContract;
import com.riskscoring.chainingest.client.dto.trongrid.TronContractValue;
import com.riskscoring.chainingest.client.dto.trongrid.TronParameter;
import com.riskscoring.chainingest.client.dto.trongrid.TronRawData;
import com.riskscoring.chainingest.client.dto.trongrid.TronRet;
import com.riskscoring.chainingest.client.dto.trongrid.TronTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TronValues implements ChainAddressValues {

    public static final String TRANSFER_CONTRACT = "TransferContract";
    public static final String TRIGGER_SMART_CONTRACT = "TriggerSmartContract";

    private static final String SUCCESS = "SUCCESS";
    private static final String BASE58_PREFIX = "T";
    private static final long NO_TIMESTAMP = 0L;

    private final TronAddressCodec addressCodec;

    public String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String address(String hexOrBase58) {
        String normalized = normalize(hexOrBase58);
        return normalized.startsWith(BASE58_PREFIX) ? normalized : addressCodec.toBase58(normalized);
    }

    @Override
    public boolean isRoutable(String address) {
        return !address.isEmpty();
    }

    public Instant timestamp(long epochMillis) {
        return epochMillis == NO_TIMESTAMP ? null : Instant.ofEpochMilli(epochMillis);
    }

    public boolean succeeded(TronTransaction transaction) {
        return Objects.requireNonNullElse(transaction.ret(), List.<TronRet>of()).stream()
                .map(TronRet::contractRet)
                .allMatch(result -> result == null || SUCCESS.equals(result));
    }

    public Optional<TronContract> contract(TronTransaction transaction) {
        return Optional.ofNullable(transaction.rawData())
                .map(TronRawData::contract)
                .filter(contracts -> !contracts.isEmpty())
                .map(List::getFirst);
    }

    public Optional<TronContractValue> value(TronContract contract) {
        return Optional.ofNullable(contract.parameter()).map(TronParameter::value);
    }

    public String scaled(String rawAmount, int decimals) {
        return scaledAmount(rawAmount, decimals).toPlainString();
    }

    private BigDecimal scaledAmount(String rawAmount, int decimals) {
        return decimal(rawAmount)
                .map(amount -> amount.movePointLeft(decimals))
                .orElse(BigDecimal.ZERO);
    }

    public BigInteger rawAmount(String rawAmount) {
        return decimal(rawAmount)
                .map(BigDecimal::toBigInteger)
                .orElse(BigInteger.ZERO);
    }

    private Optional<BigDecimal> decimal(String rawAmount) {
        return Optional.ofNullable(rawAmount)
                .filter(amount -> !amount.isBlank())
                .flatMap(amount -> {
                    try {
                        return Optional.of(new BigDecimal(amount));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                });
    }
}
