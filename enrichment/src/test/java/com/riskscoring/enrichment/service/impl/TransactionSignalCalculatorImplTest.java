package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.FlaggedExposure;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.LabelCategory;
import com.riskscoring.common.model.MixerExposure;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TransactionEvidence;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import com.riskscoring.common.model.TransactionSnapshot;
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

class TransactionSignalCalculatorImplTest {

    private static final Chain CHAIN = Chain.ETHEREUM;
    private static final String TARGET = "0xhash";
    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");
    private static final EnrichmentProperties PROPERTIES = new EnrichmentProperties(30, 50, 5);

    private TransactionSignalCalculatorImpl calculator;

    @BeforeEach
    void setUp() {
        calculator = new TransactionSignalCalculatorImpl(PROPERTIES, new Labels());
    }

    @Test
    void calculateMapsSnapshotFieldsIntoTransactionEvidence() {
        TransactionSnapshot snapshot = transaction("0xfrom", "0xto", "100", true, 0, 0, List.of());

        TransactionEvidence evidence = calculator.calculate(event(new TransactionFacts(snapshot)),
                new TransactionFacts(snapshot), Map.of());

        assertThat(evidence.target()).isEqualTo(TARGET);
        assertThat(evidence.chain()).isEqualTo(CHAIN);
        assertThat(evidence.fromAddress()).isEqualTo("0xfrom");
        assertThat(evidence.toAddress()).isEqualTo("0xto");
        assertThat(evidence.valueNative()).isEqualTo("100");
        assertThat(evidence.success()).isTrue();
    }

    @Test
    void heuristicsFailedTrueWhenTransactionUnsuccessful() {
        TransactionEvidence evidence = evidenceFor(transaction("0xfrom", "0xto", "100", false, 0, 0, List.of()));

        assertThat(evidence.heuristics().failed()).isTrue();
    }

    @Test
    void heuristicsZeroValueTrueWhenValueZeroAndNoTokenTransfers() {
        TransactionEvidence evidence = evidenceFor(transaction("0xfrom", "0xto", "0", true, 0, 0, List.of()));

        assertThat(evidence.heuristics().zeroValue()).isTrue();
        assertThat(evidence.heuristics().tokenOnly()).isFalse();
    }

    @Test
    void heuristicsTokenOnlyTrueWhenValueZeroAndTokenTransfersPresent() {
        TransactionEvidence evidence = evidenceFor(transaction("0xfrom", "0xto", "0", true, 0, 2, List.of()));

        assertThat(evidence.heuristics().tokenOnly()).isTrue();
        assertThat(evidence.heuristics().zeroValue()).isFalse();
    }

    @Test
    void heuristicsRoundValueDelegatesToChainNativeUnit() {
        String oneEther = Chain.ETHEREUM.nativeUnit().toString();
        TransactionEvidence evidence = evidenceFor(transaction("0xfrom", "0xto", oneEther, true, 0, 0, List.of()));

        assertThat(evidence.heuristics().roundValue()).isTrue();
    }

    @Test
    void selfTransferTrueWhenFromEqualsTo() {
        TransactionEvidence evidence = evidenceFor(transaction("0xsame", "0xsame", "100", true, 0, 0, List.of()));

        assertThat(evidence.heuristics().selfTransfer()).isTrue();
    }

    @Test
    void selfTransferFalseWhenFromIsNull() {
        TransactionEvidence evidence = evidenceFor(transaction(null, "0xto", "100", true, 0, 0, List.of()));

        assertThat(evidence.heuristics().selfTransfer()).isFalse();
    }

    @Test
    void selfTransferFalseWhenAddressesDiffer() {
        TransactionEvidence evidence = evidenceFor(transaction("0xfrom", "0xto", "100", true, 0, 0, List.of()));

        assertThat(evidence.heuristics().selfTransfer()).isFalse();
    }

    @Test
    void fanOutInternalTrueAtThresholdBoundary() {
        TransactionEvidence evidence = evidenceFor(transaction("0xfrom", "0xto", "100", true, 5, 0, List.of()));

        assertThat(evidence.heuristics().fanOutInternal()).isTrue();
    }

    @Test
    void fanOutInternalFalseBelowThreshold() {
        TransactionEvidence evidence = evidenceFor(transaction("0xfrom", "0xto", "100", true, 4, 0, List.of()));

        assertThat(evidence.heuristics().fanOutInternal()).isFalse();
    }

    @Test
    void distinctPartyCountDedupsByAddress() {
        List<TransactionParty> parties = List.of(
                party("0xsender", TransactionRole.SENDER, "100"),
                party("0xrecipient", TransactionRole.RECIPIENT, "100"),
                party("0xrecipient", TransactionRole.TOKEN_RECIPIENT, "50"));
        TransactionEvidence evidence = evidenceFor(transaction("0xsender", "0xrecipient", "100", true, 0, 1, parties));

        assertThat(evidence.heuristics().distinctPartyCount()).isEqualTo(2);
    }

    @Test
    void senderRoleMapsToOutDirectionAndDirectHops() {
        assertRoleMapsTo(TransactionRole.SENDER, TransferDirection.OUT, 0);
    }

    @Test
    void recipientRoleMapsToInDirectionAndDirectHops() {
        assertRoleMapsTo(TransactionRole.RECIPIENT, TransferDirection.IN, 0);
    }

    @Test
    void internalSenderRoleMapsToOutDirectionAndNestedHops() {
        assertRoleMapsTo(TransactionRole.INTERNAL_SENDER, TransferDirection.OUT, 1);
    }

    @Test
    void internalRecipientRoleMapsToInDirectionAndNestedHops() {
        assertRoleMapsTo(TransactionRole.INTERNAL_RECIPIENT, TransferDirection.IN, 1);
    }

    @Test
    void tokenSenderRoleMapsToOutDirectionAndNestedHops() {
        assertRoleMapsTo(TransactionRole.TOKEN_SENDER, TransferDirection.OUT, 1);
    }

    @Test
    void tokenRecipientRoleMapsToInDirectionAndNestedHops() {
        assertRoleMapsTo(TransactionRole.TOKEN_RECIPIENT, TransferDirection.IN, 1);
    }

    private void assertRoleMapsTo(TransactionRole role, TransferDirection expectedDirection, int expectedHops) {
        Label label = label(LabelCategory.SANCTION);
        TransactionParty party = party("0xparty", role, "77");
        TransactionSnapshot snapshot = transaction("0xfrom", "0xto", "100", true, 0, 0, List.of(party));

        TransactionEvidence evidence = calculator.calculate(event(new TransactionFacts(snapshot)),
                new TransactionFacts(snapshot), Map.of("0xparty", label));

        assertThat(evidence.flagged()).containsExactly(
                new FlaggedExposure("0xparty", LabelCategory.SANCTION, label.getName(), label.getSource(),
                        expectedDirection, expectedHops, "77"));
    }

    @Test
    void flaggedExposuresExcludesExchangeLabeledParty() {
        Label label = label(LabelCategory.EXCHANGE);
        TransactionParty party = party("0xparty", TransactionRole.SENDER, "77");
        TransactionSnapshot snapshot = transaction("0xfrom", "0xto", "100", true, 0, 0, List.of(party));

        TransactionEvidence evidence = calculator.calculate(event(new TransactionFacts(snapshot)),
                new TransactionFacts(snapshot), Map.of("0xparty", label));

        assertThat(evidence.flagged()).isEmpty();
    }

    @Test
    void mixerExposureIsNullWhenNoMixerParties() {
        TransactionParty party = party("0xparty", TransactionRole.SENDER, "77");
        TransactionEvidence evidence = evidenceFor(transaction("0xfrom", "0xto", "100", true, 0, 0, List.of(party)));

        assertThat(evidence.mixerExposure()).isNull();
    }

    @Test
    void mixerExposureComputesPercentOfTotalVolumeAndDedupsServiceNames() {
        Label mixerLabel = label(LabelCategory.MIXER);
        TransactionParty mixerOne = party("0xmixer1", TransactionRole.INTERNAL_RECIPIENT, "30");
        TransactionParty mixerTwo = party("0xmixer2", TransactionRole.INTERNAL_RECIPIENT, "20");
        TransactionParty clean = party("0xclean", TransactionRole.RECIPIENT, "50");
        TransactionSnapshot snapshot = transaction("0xfrom", "0xto", "100", true, 2, 0,
                List.of(mixerOne, mixerTwo, clean));

        TransactionEvidence evidence = calculator.calculate(event(new TransactionFacts(snapshot)),
                new TransactionFacts(snapshot), Map.of("0xmixer1", mixerLabel, "0xmixer2", mixerLabel));

        MixerExposure exposure = evidence.mixerExposure();
        assertThat(exposure).isNotNull();
        assertThat(exposure.services()).containsExactly(mixerLabel.getName());
        assertThat(exposure.percentOfVolume()).isEqualTo(50);
        assertThat(exposure.valueNative()).isEqualTo("50");
    }

    private TransactionEvidence evidenceFor(TransactionSnapshot snapshot) {
        TransactionFacts facts = new TransactionFacts(snapshot);
        return calculator.calculate(event(facts), facts, Map.of());
    }

    private static ChainFetched event(TransactionFacts facts) {
        return new ChainFetched(UUID.randomUUID(), ScanTarget.TRANSACTION, TARGET, CHAIN, facts, Language.EN, NOW);
    }

    private static TransactionSnapshot transaction(String from, String to, String value, boolean success,
                                                    int nestedTransferCount, int tokenTransferCount,
                                                    List<TransactionParty> parties) {
        return new TransactionSnapshot(TARGET, from, to, value, success, NOW, parties,
                nestedTransferCount, tokenTransferCount, List.of(), NOW);
    }

    private static TransactionParty party(String address, TransactionRole role, String value) {
        return new TransactionParty(address, role, value);
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
