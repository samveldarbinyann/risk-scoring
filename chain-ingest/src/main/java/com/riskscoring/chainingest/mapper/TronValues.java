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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TronValues {

    public static final String TRANSFER_CONTRACT = "TransferContract";
    public static final String TRIGGER_SMART_CONTRACT = "TriggerSmartContract";

    private static final String SUCCESS = "SUCCESS";
    private static final String BASE58_PREFIX = "T";
    private static final long NO_TIMESTAMP = 0L;
    private static final String ZERO = "0";

    private final TronAddressCodec addressCodec;

    public String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public String address(String hexOrBase58) {
        String normalized = normalize(hexOrBase58);
        return normalized.startsWith(BASE58_PREFIX) ? normalized : addressCodec.toBase58(normalized);
    }

    public boolean isRoutable(String address) {
        return !address.isEmpty();
    }

    public Instant timestamp(long epochMillis) {
        return epochMillis == NO_TIMESTAMP ? null : Instant.ofEpochMilli(epochMillis);
    }

    public boolean succeeded(TronTransaction transaction) {
        return Optional.ofNullable(transaction.ret()).orElseGet(List::of).stream()
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
        return Optional.ofNullable(rawAmount)
                .filter(amount -> !amount.isBlank())
                .map(amount -> new BigDecimal(amount).movePointLeft(decimals).toPlainString())
                .orElse(ZERO);
    }
}
