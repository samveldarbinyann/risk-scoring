package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.FlaggedExposure;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.LabelCategory;
import com.riskscoring.common.model.MixerExposure;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TokenBalance;
import com.riskscoring.common.model.TransferDirection;
import com.riskscoring.enrichment.config.EnrichmentProperties;
import com.riskscoring.enrichment.entity.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddressSignalCalculatorImplTest {

    private static final Chain CHAIN = Chain.ETHEREUM;
    private static final String TARGET = "0xtarget";
    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");
    private static final EnrichmentProperties PROPERTIES = new EnrichmentProperties(30, 50, 5, null);

    private AddressSignalCalculatorImpl calculator;

    @BeforeEach
    void setUp() {
        calculator = new AddressSignalCalculatorImpl(PROPERTIES, new Labels());
    }

    @Test
    void calculateMapsSnapshotFieldsIntoAddressEvidence() {
        List<TokenBalance> balances = List.of(new TokenBalance("USDC", "10", 10.0));
        AddressSnapshot snapshot = new AddressSnapshot(7, 2, "1000", balances, NOW.minusSeconds(86400), NOW, true, NOW);
        Counterparty counterparty = counterparty("0xcp1", TransferDirection.IN, "100", 1);
        AddressFacts facts = new AddressFacts(snapshot, List.of(counterparty));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.target()).isEqualTo(TARGET);
        assertThat(evidence.chain()).isEqualTo(CHAIN);
        assertThat(evidence.observedAt()).isEqualTo(NOW);
        assertThat(evidence.txCount()).isEqualTo(7);
        assertThat(evidence.txCount24h()).isEqualTo(2);
        assertThat(evidence.sampleTruncated()).isTrue();
        assertThat(evidence.balanceNative()).isEqualTo("1000");
        assertThat(evidence.tokenBalances()).isEqualTo(balances);
        assertThat(evidence.counterpartyCount()).isEqualTo(1);
    }

    @Test
    void ageDaysIsNullWhenFirstSeenAtIsNull() {
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of());

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.ageDays()).isNull();
        assertThat(evidence.heuristics().freshWallet()).isNull();
        assertThat(evidence.heuristics().fundedThenDrained()).isNull();
    }

    @Test
    void ageDaysTruncatesPartialDays() {
        Instant firstSeenAt = NOW.minusSeconds((29 * 24 + 23) * 3600L);
        AddressFacts facts = addressFacts(snapshot(firstSeenAt, NOW, "1000"), List.of());

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.ageDays()).isEqualTo(29);
    }

    @Test
    void freshWalletTrueWhenAgeBelowThreshold() {
        Instant firstSeenAt = NOW.minusSeconds(29 * 86400L);
        AddressFacts facts = addressFacts(snapshot(firstSeenAt, NOW, "1000"), List.of());

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().freshWallet()).isTrue();
    }

    @Test
    void freshWalletFalseAtThresholdBoundary() {
        Instant firstSeenAt = NOW.minusSeconds(30 * 86400L);
        AddressFacts facts = addressFacts(snapshot(firstSeenAt, NOW, "1000"), List.of());

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().freshWallet()).isFalse();
    }

    @Test
    void fundedThenDrainedTrueWhenFreshAndDrainedAndFanInPositive() {
        Instant firstSeenAt = NOW.minusSeconds(5 * 86400L);
        Counterparty incoming = counterparty("0xcp1", TransferDirection.IN, "500", 1);
        AddressFacts facts = addressFacts(snapshot(firstSeenAt, NOW, "0"), List.of(incoming));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().fundedThenDrained()).isTrue();
    }

    @Test
    void fundedThenDrainedFalseWhenBalanceNonZero() {
        Instant firstSeenAt = NOW.minusSeconds(5 * 86400L);
        Counterparty incoming = counterparty("0xcp1", TransferDirection.IN, "500", 1);
        AddressFacts facts = addressFacts(snapshot(firstSeenAt, NOW, "100"), List.of(incoming));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().fundedThenDrained()).isFalse();
    }

    @Test
    void fundedThenDrainedFalseWhenFanInIsZero() {
        Instant firstSeenAt = NOW.minusSeconds(5 * 86400L);
        Counterparty outgoingOnly = counterparty("0xcp1", TransferDirection.OUT, "500", 1);
        AddressFacts facts = addressFacts(snapshot(firstSeenAt, NOW, "0"), List.of(outgoingOnly));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().fundedThenDrained()).isFalse();
    }

    @Test
    void fundedThenDrainedNullWhenFreshWalletIsNull() {
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of());

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().fundedThenDrained()).isNull();
    }

    @Test
    void roundAmountsFalseWhenCounterpartiesEmpty() {
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of());

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().roundAmounts()).isFalse();
    }

    @Test
    void roundAmountsTrueAtThresholdBoundary() {
        String oneEther = Chain.ETHEREUM.nativeUnit().toString();
        Counterparty round = counterparty("0xcp1", TransferDirection.IN, oneEther, 1);
        Counterparty notRound = counterparty("0xcp2", TransferDirection.IN, "1", 1);
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of(round, notRound));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().roundAmounts()).isTrue();
    }

    @Test
    void roundAmountsFalseBelowThreshold() {
        String oneEther = Chain.ETHEREUM.nativeUnit().toString();
        Counterparty round = counterparty("0xcp1", TransferDirection.IN, oneEther, 1);
        Counterparty notRound1 = counterparty("0xcp2", TransferDirection.IN, "1", 1);
        Counterparty notRound2 = counterparty("0xcp3", TransferDirection.IN, "2", 1);
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of(round, notRound1, notRound2));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().roundAmounts()).isFalse();
    }

    @Test
    void fanInAndFanOutBothCountBothDirectionCounterparty() {
        Counterparty both = counterparty("0xcp1", TransferDirection.BOTH, "100", 1);
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of(both));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().fanIn()).isEqualTo(1);
        assertThat(evidence.heuristics().fanOut()).isEqualTo(1);
    }

    @Test
    void fanInCountsOnlyInDirectionCounterparties() {
        Counterparty in = counterparty("0xcp1", TransferDirection.IN, "100", 1);
        Counterparty out = counterparty("0xcp2", TransferDirection.OUT, "100", 1);
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of(in, out));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.heuristics().fanIn()).isEqualTo(1);
        assertThat(evidence.heuristics().fanOut()).isEqualTo(1);
    }

    @Test
    void flaggedExposuresIncludesTargetWhenSanctioned() {
        Label label = label(LabelCategory.SANCTION);
        AddressFacts facts = addressFacts(snapshot(null, NOW, "1000"), List.of());

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of(TARGET, label));

        assertThat(evidence.flagged()).containsExactly(
                new FlaggedExposure(TARGET, LabelCategory.SANCTION, label.getName(), label.getSource(),
                        TransferDirection.BOTH, 0, "1000"));
    }

    @Test
    void flaggedExposuresIncludesFlaggedCounterpartyWithItsOwnDirectionAndHops() {
        Label label = label(LabelCategory.MIXER);
        Counterparty counterparty = counterparty("0xcp1", TransferDirection.OUT, "250", 2);
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of(counterparty));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of("0xcp1", label));

        assertThat(evidence.flagged()).containsExactly(
                new FlaggedExposure("0xcp1", LabelCategory.MIXER, label.getName(), label.getSource(),
                        TransferDirection.OUT, 2, "250"));
    }

    @Test
    void flaggedExposuresExcludesExchangeLabeledCounterparty() {
        Label label = label(LabelCategory.EXCHANGE);
        Counterparty counterparty = counterparty("0xcp1", TransferDirection.OUT, "250", 1);
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of(counterparty));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of("0xcp1", label));

        assertThat(evidence.flagged()).isEmpty();
    }

    @Test
    void flaggedExposuresEmptyWhenNoLabelsMatch() {
        Counterparty counterparty = counterparty("0xcp1", TransferDirection.OUT, "250", 1);
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of(counterparty));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.flagged()).isEmpty();
    }

    @Test
    void mixerExposureIsNullWhenNoMixerCounterparties() {
        Counterparty counterparty = counterparty("0xcp1", TransferDirection.OUT, "250", 1);
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of(counterparty));

        AddressEvidence evidence = calculator.calculate(event(facts), facts, Map.of());

        assertThat(evidence.mixerExposure()).isNull();
    }

    @Test
    void mixerExposureComputesPercentOfTotalVolumeAndDedupsServiceNames() {
        Label mixerLabel = label(LabelCategory.MIXER);
        Counterparty mixerOne = counterparty("0xmixer1", TransferDirection.OUT, "30", 1);
        Counterparty mixerTwo = counterparty("0xmixer2", TransferDirection.OUT, "20", 1);
        Counterparty clean = counterparty("0xcp1", TransferDirection.IN, "50", 1);
        AddressFacts facts = addressFacts(snapshot(null, NOW, "0"), List.of(mixerOne, mixerTwo, clean));

        AddressEvidence evidence = calculator.calculate(event(facts), facts,
                Map.of("0xmixer1", mixerLabel, "0xmixer2", mixerLabel));

        MixerExposure exposure = evidence.mixerExposure();
        assertThat(exposure).isNotNull();
        assertThat(exposure.services()).containsExactly(mixerLabel.getName());
        assertThat(exposure.percentOfVolume()).isEqualTo(50);
        assertThat(exposure.valueNative()).isEqualTo("50");
    }

    private static ChainFetched event(AddressFacts facts) {
        return new ChainFetched(UUID.randomUUID(), ScanTarget.ADDRESS, TARGET, CHAIN, facts, Language.EN, NOW);
    }

    private static AddressFacts addressFacts(AddressSnapshot snapshot, List<Counterparty> counterparties) {
        return new AddressFacts(snapshot, counterparties);
    }

    private static AddressSnapshot snapshot(Instant firstSeenAt, Instant observedAt, String balanceNative) {
        return new AddressSnapshot(1, 1, balanceNative, List.of(), firstSeenAt, observedAt, false, observedAt);
    }

    private static Counterparty counterparty(String address, TransferDirection direction, String totalValueNative, int hops) {
        return new Counterparty(address, direction, 1, totalValueNative, hops);
    }

    private static Label label(LabelCategory category) {
        return Label.builder()
                .id(UUID.randomUUID())
                .chain(CHAIN)
                .address("irrelevant")
                .category(category)
                .name("Test Label")
                .source("Test Source")
                .updatedAt(Instant.now())
                .build();
    }
}
