package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.TransactionEvidence;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionHeuristics;
import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import com.riskscoring.common.model.TransactionSnapshot;
import com.riskscoring.common.model.TransferDirection;
import com.riskscoring.enrichment.entity.Label;
import com.riskscoring.enrichment.service.AddressSignalCalculator;
import com.riskscoring.enrichment.service.TransactionSignalCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskSignalCalculatorImplTest {

    private static final Chain CHAIN = Chain.ETHEREUM;
    private static final String TARGET = "0xtarget";
    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");

    @Mock
    private AddressSignalCalculator addressSignalCalculator;
    @Mock
    private TransactionSignalCalculator transactionSignalCalculator;

    private RiskSignalCalculatorImpl calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskSignalCalculatorImpl(addressSignalCalculator, transactionSignalCalculator);
    }

    @Test
    void addressesToLabelCollectsTargetAndCounterpartiesInOrderWithoutDuplicates() {
        AddressFacts facts = new AddressFacts(addressSnapshot(), List.of(
                new Counterparty("0xcp1", TransferDirection.IN, 1, "100", 1),
                new Counterparty("0xcp2", TransferDirection.OUT, 1, "100", 1),
                new Counterparty(TARGET, TransferDirection.IN, 1, "100", 1)));

        Set<String> addresses = calculator.addressesToLabel(event(facts));

        assertThat(addresses).containsExactly(TARGET, "0xcp1", "0xcp2");
    }

    @Test
    void addressesToLabelCollectsDistinctTransactionPartyAddresses() {
        TransactionSnapshot transaction = transactionSnapshot(List.of(
                new TransactionParty("0xsender", TransactionRole.SENDER, "100"),
                new TransactionParty("0xrecipient", TransactionRole.RECIPIENT, "100"),
                new TransactionParty("0xsender", TransactionRole.INTERNAL_SENDER, "50")));
        TransactionFacts facts = new TransactionFacts(transaction);

        Set<String> addresses = calculator.addressesToLabel(event(facts));

        assertThat(addresses).containsExactly("0xsender", "0xrecipient");
    }

    @Test
    void calculateDispatchesToAddressSignalCalculatorForAddressFacts() {
        AddressFacts facts = new AddressFacts(addressSnapshot(), List.of());
        ChainFetched event = event(facts);
        Map<String, Label> labels = Map.of();
        AddressEvidence expected = addressEvidence();
        when(addressSignalCalculator.calculate(event, facts, labels)).thenReturn(expected);

        EvidenceBundle result = calculator.calculate(event, labels);

        assertThat(result).isEqualTo(expected);
        verifyNoInteractions(transactionSignalCalculator);
    }

    @Test
    void calculateDispatchesToTransactionSignalCalculatorForTransactionFacts() {
        TransactionFacts facts = new TransactionFacts(transactionSnapshot(List.of()));
        ChainFetched event = event(facts);
        Map<String, Label> labels = Map.of();
        TransactionEvidence expected = transactionEvidence();
        when(transactionSignalCalculator.calculate(event, facts, labels)).thenReturn(expected);

        EvidenceBundle result = calculator.calculate(event, labels);

        assertThat(result).isEqualTo(expected);
        verifyNoInteractions(addressSignalCalculator);
    }

    private static ChainFetched event(AddressFacts facts) {
        return new ChainFetched(UUID.randomUUID(), ScanTarget.ADDRESS, TARGET, CHAIN, facts, Language.EN, NOW);
    }

    private static ChainFetched event(TransactionFacts facts) {
        return new ChainFetched(UUID.randomUUID(), ScanTarget.TRANSACTION, TARGET, CHAIN, facts, Language.EN, NOW);
    }

    private static AddressSnapshot addressSnapshot() {
        return new AddressSnapshot(1, 1, "0", List.of(), null, NOW, false, NOW);
    }

    private static TransactionSnapshot transactionSnapshot(List<TransactionParty> parties) {
        return new TransactionSnapshot("0xhash", "0xfrom", "0xto", "0", true, NOW, parties, 0, 0, List.of(), NOW);
    }

    private static AddressEvidence addressEvidence() {
        return new AddressEvidence(TARGET, CHAIN, NOW, null, 0, 0, false, "0", List.of(), 0, List.of(), null,
                new Heuristics(null, null, false, 0, 0));
    }

    private static TransactionEvidence transactionEvidence() {
        return new TransactionEvidence(TARGET, CHAIN, NOW, "0xfrom", "0xto", "0", true, NOW, 0, 0, List.of(),
                List.of(), List.of(), null,
                new TransactionHeuristics(false, true, false, false, false, false, 0));
    }
}
