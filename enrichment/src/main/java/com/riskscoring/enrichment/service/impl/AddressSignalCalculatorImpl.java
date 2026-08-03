package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.FlaggedExposure;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.LabelCategory;
import com.riskscoring.common.model.MixerExposure;
import com.riskscoring.common.model.TransferDirection;
import com.riskscoring.enrichment.config.EnrichmentProperties;
import com.riskscoring.enrichment.entity.Label;
import com.riskscoring.enrichment.service.AddressSignalCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddressSignalCalculatorImpl implements AddressSignalCalculator {

    private static final int SELF_HOPS = 0;

    private final EnrichmentProperties properties;
    private final Labels labels;

    @Override
    public AddressEvidence calculate(ChainFetched event, AddressFacts facts, Map<String, Label> labelsByAddress) {
        AddressSnapshot snapshot = facts.snapshot();
        List<Counterparty> counterparties = facts.counterparties();
        int ageDays = ageDays(snapshot);

        return new AddressEvidence(
                event.target(),
                event.chain(),
                snapshot.observedAt(),
                ageDays,
                snapshot.txCount(),
                snapshot.txCount24h(),
                snapshot.sampleTruncated(),
                snapshot.balanceNative(),
                snapshot.tokenBalances(),
                counterparties.size(),
                flaggedExposures(event, facts, labelsByAddress),
                mixerExposure(counterparties, labelsByAddress),
                heuristics(snapshot, counterparties, ageDays, event.chain())
        );
    }

    private int ageDays(AddressSnapshot snapshot) {
        return Optional.ofNullable(snapshot.firstSeenAt())
                .map(firstSeenAt -> (int) Duration.between(firstSeenAt, snapshot.observedAt()).toDays())
                .orElse(0);
    }

    private List<FlaggedExposure> flaggedExposures(ChainFetched event, AddressFacts facts, Map<String, Label> byAddress) {
        List<FlaggedExposure> exposures = new ArrayList<>();

        labels.flagged(byAddress, event.target())
                .map(label -> labels.toExposure(label, event.target(), TransferDirection.BOTH,
                        SELF_HOPS, facts.snapshot().balanceNative()))
                .ifPresent(exposures::add);

        facts.counterparties().forEach(counterparty ->
                labels.flagged(byAddress, counterparty.address())
                        .map(label -> labels.toExposure(label, counterparty.address(), counterparty.direction(),
                                counterparty.hops(), counterparty.totalValueNative()))
                        .ifPresent(exposures::add));

        return List.copyOf(exposures);
    }

    private MixerExposure mixerExposure(List<Counterparty> counterparties, Map<String, Label> byAddress) {
        List<Counterparty> throughMixers = counterparties.stream()
                .filter(counterparty -> labels.hasCategory(byAddress, counterparty.address(), LabelCategory.MIXER))
                .toList();

        if (throughMixers.isEmpty()) {
            return null;
        }

        BigInteger mixerVolume = totalVolume(throughMixers);
        List<String> services = throughMixers.stream()
                .map(counterparty -> byAddress.get(counterparty.address()).getName())
                .distinct()
                .toList();

        return new MixerExposure(services, labels.percent(mixerVolume, totalVolume(counterparties)), mixerVolume.toString());
    }

    private Heuristics heuristics(AddressSnapshot snapshot,
                                  List<Counterparty> counterparties,
                                  int ageDays,
                                  Chain chain) {
        boolean freshWallet = ageDays < properties.freshWalletDays();
        boolean drained = new BigInteger(snapshot.balanceNative()).signum() == 0;

        int fanIn = countByDirection(counterparties, TransferDirection.IN);
        int fanOut = countByDirection(counterparties, TransferDirection.OUT);

        return new Heuristics(
                freshWallet,
                freshWallet && drained && fanIn > 0,
                roundAmounts(counterparties, chain),
                fanIn,
                fanOut
        );
    }

    private boolean roundAmounts(List<Counterparty> counterparties, Chain chain) {
        if (counterparties.isEmpty()) {
            return false;
        }

        BigInteger nativeUnit = BigInteger.TEN.pow(chain.nativeDecimals());

        long round = counterparties.stream()
                .map(counterparty -> new BigInteger(counterparty.totalValueNative()))
                .filter(value -> value.signum() > 0 && value.mod(nativeUnit).signum() == 0)
                .count();

        return round * Labels.PERCENT / counterparties.size() >= properties.roundAmountsPercentThreshold();
    }

    private int countByDirection(List<Counterparty> counterparties, TransferDirection direction) {
        return (int) counterparties.stream()
                .filter(counterparty -> counterparty.direction() == direction
                        || counterparty.direction() == TransferDirection.BOTH)
                .count();
    }

    private BigInteger totalVolume(List<Counterparty> counterparties) {
        return counterparties.stream()
                .map(counterparty -> new BigInteger(counterparty.totalValueNative()))
                .reduce(BigInteger.ZERO, BigInteger::add);
    }
}
