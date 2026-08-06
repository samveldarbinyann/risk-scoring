package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.*;
import com.riskscoring.enrichment.entity.Label;
import com.riskscoring.enrichment.service.AddressSignalCalculator;
import com.riskscoring.enrichment.service.RiskSignalCalculator;
import com.riskscoring.enrichment.service.TransactionSignalCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RiskSignalCalculatorImpl implements RiskSignalCalculator {

    private final AddressSignalCalculator addressSignalCalculator;
    private final TransactionSignalCalculator transactionSignalCalculator;

    @Override
    public Set<String> addressesToLabel(ChainFetched event) {
        Stream<String> addresses = switch (event.facts()) {
            case AddressFacts facts -> Stream.concat(
                    Stream.of(event.target()),
                    facts.counterparties().stream().map(Counterparty::address));
            case TransactionFacts facts -> facts.transaction().parties().stream()
                    .map(TransactionParty::address);
        };

        return addresses.collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public EvidenceBundle calculate(ChainFetched event, Map<String, Label> labelsByAddress) {
        return switch (event.facts()) {
            case AddressFacts facts -> addressSignalCalculator.calculate(event, facts, labelsByAddress);
            case TransactionFacts facts -> transactionSignalCalculator.calculate(event, facts, labelsByAddress);
        };
    }
}
