package com.riskscoring.chainingest.client.impl;

import com.riskscoring.chainingest.client.ChainData;
import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.config.ChainIngestProperties;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.TransferDirection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class StubChainDataClient implements ChainDataClient {

    private static final int MAX_AGE_DAYS = 1500;
    private static final int MAX_TX_COUNT = 5000;
    private static final BigInteger WEI_IN_ETHER = BigInteger.TEN.pow(18);

    private final ChainIngestProperties properties;

    @Override
    public ChainData fetch(String address, int chainId) {
        log.warn("Using stub chain data for address={} chainId={}", address, chainId);

        Random random = new Random(address.hashCode() + chainId);
        Instant now = Instant.now();

        int ageDays = random.nextInt(MAX_AGE_DAYS) + 1;
        Instant firstSeenAt = now.minus(Duration.ofDays(ageDays));

        AddressSnapshot snapshot = new AddressSnapshot(
                ageDays,
                random.nextInt(MAX_TX_COUNT),
                randomWei(random).toString(),
                firstSeenAt,
                now.minus(Duration.ofDays(random.nextInt(ageDays)))
        );

        return new ChainData(snapshot, randomCounterparties(random));
    }

    private List<Counterparty> randomCounterparties(Random random) {
        int count = random.nextInt(properties.maxCounterparties()) + 1;

        return IntStream.range(0, count)
                .mapToObj(index -> new Counterparty(
                        randomAddress(random),
                        TransferDirection.values()[random.nextInt(TransferDirection.values().length)],
                        random.nextInt(100) + 1,
                        randomWei(random).toString(),
                        random.nextInt(2) + 1
                ))
                .toList();
    }

    private BigInteger randomWei(Random random) {
        return BigInteger.valueOf(random.nextInt(1000)).multiply(WEI_IN_ETHER);
    }

    private String randomAddress(Random random) {
        StringBuilder builder = new StringBuilder("0x");
        IntStream.range(0, 40).forEach(index -> builder.append(Integer.toHexString(random.nextInt(16))));
        return builder.toString();
    }
}