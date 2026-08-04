package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.mempool.MempoolStatus;
import com.riskscoring.chainingest.client.dto.mempool.MempoolTransaction;
import com.riskscoring.chainingest.client.dto.mempool.MempoolVin;
import com.riskscoring.chainingest.client.dto.mempool.MempoolVout;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class BitcoinValues implements ChainAddressValues {

    public String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String address(String value) {
        return normalize(value);
    }

    @Override
    public boolean isRoutable(String address) {
        return !address.isEmpty();
    }

    public Instant timestamp(MempoolStatus status) {
        return Optional.ofNullable(status)
                .map(MempoolStatus::blockTime)
                .map(Instant::ofEpochSecond)
                .orElse(null);
    }

    public List<MempoolVin> inputs(MempoolTransaction transaction) {
        return Optional.ofNullable(transaction.vin()).orElseGet(List::of);
    }

    public List<MempoolVout> outputs(MempoolTransaction transaction) {
        return Optional.ofNullable(transaction.vout()).orElseGet(List::of);
    }

    public String inputAddress(MempoolVin input) {
        return Optional.ofNullable(input.prevout()).map(MempoolVout::address).map(this::address).orElse("");
    }

    public long inputValue(MempoolVin input) {
        return Optional.ofNullable(input.prevout()).map(MempoolVout::value).orElse(0L);
    }
}
