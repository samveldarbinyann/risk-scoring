package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.Counterparty;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.FlaggedExposure;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.LabelCategory;
import com.riskscoring.common.model.MixerExposure;
import com.riskscoring.common.model.TransferDirection;
import com.riskscoring.enrichment.config.EnrichmentProperties;
import com.riskscoring.enrichment.entity.Label;
import com.riskscoring.enrichment.service.RiskSignalCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RiskSignalCalculatorImpl implements RiskSignalCalculator {

    private static final Set<LabelCategory> FLAGGED_CATEGORIES =
            EnumSet.of(LabelCategory.SANCTION, LabelCategory.MIXER);
    private static final BigInteger WEI_IN_ETHER = BigInteger.TEN.pow(18);
    private static final int SELF_HOPS = 0;
    private static final int PERCENT = 100;

    private final EnrichmentProperties properties;

    @Override
    public EvidenceBundle calculate(ChainFetched event, Map<String, Label> labelsByAddress) {
        List<Counterparty> counterparties = event.counterparties();

        return new EvidenceBundle(
                event.address(),
                event.chainId(),
                event.snapshot().ageDays(),
                event.snapshot().txCount(),
                event.snapshot().balanceWei(),
                counterparties.size(),
                flaggedExposures(event, labelsByAddress),
                mixerExposure(counterparties, labelsByAddress),
                heuristics(event, counterparties)
        );
    }

    private List<FlaggedExposure> flaggedExposures(ChainFetched event, Map<String, Label> labels) {
        List<FlaggedExposure> exposures = new ArrayList<>();

        flaggedLabel(labels, event.address())
                .map(label -> toExposure(label, event.address(), TransferDirection.BOTH,
                        SELF_HOPS, event.snapshot().balanceWei()))
                .ifPresent(exposures::add);

        event.counterparties().forEach(counterparty ->
                flaggedLabel(labels, counterparty.address())
                        .map(label -> toExposure(label, counterparty.address(), counterparty.direction(),
                                counterparty.hops(), counterparty.totalValueWei()))
                        .ifPresent(exposures::add));

        return List.copyOf(exposures);
    }

    private FlaggedExposure toExposure(Label label, String address, TransferDirection direction,
                                       int hops, String valueWei) {
        return new FlaggedExposure(
                address,
                label.getCategory(),
                label.getName(),
                label.getSource(),
                direction,
                hops,
                valueWei);
    }

    private MixerExposure mixerExposure(List<Counterparty> counterparties, Map<String, Label> labels) {
        List<Counterparty> throughMixers = counterparties.stream()
                .filter(counterparty -> hasCategory(labels, counterparty.address(), LabelCategory.MIXER))
                .toList();

        if (throughMixers.isEmpty()) {
            return null;
        }

        BigInteger mixerVolume = totalVolume(throughMixers);
        BigInteger totalVolume = totalVolume(counterparties);

        int percent = totalVolume.signum() == 0
                ? 0
                : mixerVolume.multiply(BigInteger.valueOf(PERCENT)).divide(totalVolume).intValue();

        List<String> services = throughMixers.stream()
                .map(counterparty -> labels.get(counterparty.address()).getName())
                .distinct()
                .toList();

        return new MixerExposure(services, percent, mixerVolume.toString());
    }

    private Heuristics heuristics(ChainFetched event, List<Counterparty> counterparties) {
        boolean freshWallet = event.snapshot().ageDays() < properties.freshWalletDays();
        boolean drained = new BigInteger(event.snapshot().balanceWei()).signum() == 0;

        int fanIn = countByDirection(counterparties, TransferDirection.IN);
        int fanOut = countByDirection(counterparties, TransferDirection.OUT);

        return new Heuristics(
                freshWallet,
                freshWallet && drained && fanIn > 0,
                roundAmounts(counterparties),
                fanIn,
                fanOut
        );
    }

    private boolean roundAmounts(List<Counterparty> counterparties) {
        if (counterparties.isEmpty()) {
            return false;
        }

        long round = counterparties.stream()
                .map(counterparty -> new BigInteger(counterparty.totalValueWei()))
                .filter(value -> value.signum() > 0 && value.mod(WEI_IN_ETHER).signum() == 0)
                .count();

        return round * PERCENT / counterparties.size() >= properties.roundAmountsPercentThreshold();
    }

    private int countByDirection(List<Counterparty> counterparties, TransferDirection direction) {
        return (int) counterparties.stream()
                .filter(counterparty -> counterparty.direction() == direction
                        || counterparty.direction() == TransferDirection.BOTH)
                .count();
    }

    private BigInteger totalVolume(List<Counterparty> counterparties) {
        return counterparties.stream()
                .map(counterparty -> new BigInteger(counterparty.totalValueWei()))
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    private boolean hasCategory(Map<String, Label> labels, String address, LabelCategory category) {
        return Optional.ofNullable(labels.get(address))
                .filter(label -> label.getCategory() == category)
                .isPresent();
    }

    private Optional<Label> flaggedLabel(Map<String, Label> labels, String address) {
        return Optional.ofNullable(labels.get(address))
                .filter(label -> FLAGGED_CATEGORIES.contains(label.getCategory()));
    }
}