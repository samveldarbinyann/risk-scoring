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
    private static final int ADDRESS_HEX_LENGTH = 40;
    private static final int HEX_RADIX = 16;
    private static final BigInteger WEI_IN_ETHER = BigInteger.TEN.pow(18);
    private static final TransferDirection[] DIRECTIONS = TransferDirection.values();
    private static final int PERCENT = 100;

    private static final List<String> WELL_KNOWN_ADDRESSES = List.of(
            "0x12d66f87a04a9e220743712ce6d9bb1b5616b8fc",
            "0x910cbd523d972eb0a6f4cae4618ad62622b39dbf",
            "0xa160cdab225685da1d56aa342ad8841c3b53f291",
            "0x7f367cc41522ce07553e823bf3be79a889debe1b",
            "0x098b716b8aaf21512996dc57eb0615e2383e2f96",
            "0x28c6c06298d514db089934071355e5743bf21d60",
            "0x71660c4005ba85c37ccec55d0c4493e66fe775d3"
    );

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
                        counterpartyAddress(random),
                        DIRECTIONS[random.nextInt(DIRECTIONS.length)],
                        random.nextInt(100) + 1,
                        randomWei(random).toString(),
                        random.nextInt(2) + 1
                ))
                .toList();
    }

    private String counterpartyAddress(Random random) {
        return random.nextInt(PERCENT) < properties.knownAddressPercent()
                ? WELL_KNOWN_ADDRESSES.get(random.nextInt(WELL_KNOWN_ADDRESSES.size()))
                : randomAddress(random);
    }

    private BigInteger randomWei(Random random) {
        return BigInteger.valueOf(random.nextInt(1000)).multiply(WEI_IN_ETHER);
    }

    private String randomAddress(Random random) {
        StringBuilder builder = new StringBuilder("0x");
        for (int i = 0; i < ADDRESS_HEX_LENGTH; i++) {
            builder.append(Integer.toHexString(random.nextInt(HEX_RADIX)));
        }
        return builder.toString();
    }
}