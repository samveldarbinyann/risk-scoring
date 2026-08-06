package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.entity.AddressCache;
import com.riskscoring.chainingest.entity.CounterpartyCache;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.TokenBalance;
import com.riskscoring.common.model.TransferDirection;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddressCacheMapperTest {

    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AddressCacheMapper mapper = new AddressCacheMapper(objectMapper);

    @Test
    void toFactsDeserializesTokenBalancesAndMapsCounterparties() {
        List<TokenBalance> balances = List.of(new TokenBalance("USDC", "10.5", 10.5));
        AddressCache cache = AddressCache.builder()
                .id(UUID.randomUUID())
                .chain(Chain.ETHEREUM)
                .address("0xabc")
                .txCount(5)
                .txCount24h(2)
                .balanceNative(BigInteger.valueOf(1000))
                .tokenBalances(objectMapper.writeValueAsString(balances))
                .firstSeenAt(NOW)
                .lastSeenAt(NOW)
                .sampleTruncated(false)
                .fetchedAt(NOW)
                .build();
        cache.replaceCounterparties(List.of(CounterpartyCache.builder()
                .id(UUID.randomUUID())
                .address("0xcounterparty")
                .direction(TransferDirection.OUT)
                .txCount(3)
                .totalValueNative(BigInteger.valueOf(500))
                .hops(1)
                .build()));

        AddressFacts facts = mapper.toFacts(cache);

        assertThat(facts.snapshot().txCount()).isEqualTo(5);
        assertThat(facts.snapshot().balanceNative()).isEqualTo("1000");
        assertThat(facts.snapshot().tokenBalances()).isEqualTo(balances);
        assertThat(facts.counterparties()).containsExactly(
                new Counterparty("0xcounterparty", TransferDirection.OUT, 3, "500", 1));
    }

    @Test
    void newEntityGeneratesRandomIdAndSetsChainAndAddress() {
        AddressCache first = mapper.newEntity("0xabc", Chain.BITCOIN);
        AddressCache second = mapper.newEntity("0xabc", Chain.BITCOIN);

        assertThat(first.getId()).isNotNull();
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getChain()).isEqualTo(Chain.BITCOIN);
        assertThat(first.getAddress()).isEqualTo("0xabc");
    }

    @Test
    void updateSnapshotMutatesCacheAndSerializesTokenBalancesRoundTrip() {
        AddressCache cache = mapper.newEntity("0xabc", Chain.ETHEREUM);
        List<TokenBalance> balances = List.of(new TokenBalance("USDT", "5", 5.0));
        AddressSnapshot snapshot = new AddressSnapshot(
                10, 3, "2000", balances, NOW, NOW, true, NOW);

        mapper.updateSnapshot(cache, snapshot);

        assertThat(cache.getTxCount()).isEqualTo(10);
        assertThat(cache.getTxCount24h()).isEqualTo(3);
        assertThat(cache.getBalanceNative()).isEqualTo(BigInteger.valueOf(2000));
        assertThat(cache.isSampleTruncated()).isTrue();
        assertThat(cache.getFetchedAt()).isEqualTo(NOW);
        assertThat(objectMapper.readValue(cache.getTokenBalances(), TokenBalance[].class))
                .containsExactly(balances.toArray(new TokenBalance[0]));
    }

    @Test
    void toEntitiesAssignsFreshIdsAndParsesTotalValueNative() {
        List<Counterparty> counterparties = List.of(
                new Counterparty("0xa", TransferDirection.IN, 2, "300", 1));

        List<CounterpartyCache> entities = mapper.toEntities(counterparties);

        assertThat(entities).hasSize(1);
        CounterpartyCache entity = entities.getFirst();
        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getAddress()).isEqualTo("0xa");
        assertThat(entity.getTotalValueNative()).isEqualTo(BigInteger.valueOf(300));
    }
}
