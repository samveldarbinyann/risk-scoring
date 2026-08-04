package com.riskscoring.enrichment.service.impl;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.FlaggedExposure;
import com.riskscoring.common.model.LabelCategory;
import com.riskscoring.common.model.MixerExposure;
import com.riskscoring.common.model.TransactionEvidence;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.common.model.TransactionHeuristics;
import com.riskscoring.common.model.TransactionParty;
import com.riskscoring.common.model.TransactionRole;
import com.riskscoring.common.model.TransactionSnapshot;
import com.riskscoring.common.model.TransferDirection;
import com.riskscoring.enrichment.config.EnrichmentProperties;
import com.riskscoring.enrichment.entity.Label;
import com.riskscoring.enrichment.service.TransactionSignalCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransactionSignalCalculatorImpl implements TransactionSignalCalculator {

    private static final int DIRECT_HOPS = 0;
    private static final int NESTED_HOPS = 1;

    private static final Set<TransactionRole> DIRECT_ROLES =
            EnumSet.of(TransactionRole.SENDER, TransactionRole.RECIPIENT);
    private static final Set<TransactionRole> SENDING_ROLES = EnumSet.of(
            TransactionRole.SENDER, TransactionRole.INTERNAL_SENDER, TransactionRole.TOKEN_SENDER);

    private final EnrichmentProperties properties;
    private final Labels labels;

    @Override
    public TransactionEvidence calculate(ChainFetched event, TransactionFacts facts, Map<String, Label> byAddress) {
        TransactionSnapshot transaction = facts.transaction();

        return new TransactionEvidence(
                event.target(),
                event.chain(),
                transaction.observedAt(),
                transaction.fromAddress(),
                transaction.toAddress(),
                transaction.valueNative(),
                transaction.success(),
                transaction.blockTimestamp(),
                transaction.nestedTransferCount(),
                transaction.tokenTransferCount(),
                transaction.tokenTransfers(),
                transaction.parties(),
                flaggedExposures(transaction, byAddress),
                mixerExposure(transaction, byAddress),
                heuristics(transaction, event.chain())
        );
    }

    private List<FlaggedExposure> flaggedExposures(TransactionSnapshot transaction, Map<String, Label> byAddress) {
        return transaction.parties().stream()
                .flatMap(party -> labels.flagged(byAddress, party.address())
                        .map(label -> labels.toExposure(label, party.address(), direction(party),
                                hops(party), party.valueNative()))
                        .stream())
                .toList();
    }

    private MixerExposure mixerExposure(TransactionSnapshot transaction, Map<String, Label> byAddress) {
        List<TransactionParty> throughMixers = transaction.parties().stream()
                .filter(party -> labels.hasCategory(byAddress, party.address(), LabelCategory.MIXER))
                .toList();

        if (throughMixers.isEmpty()) {
            return null;
        }

        BigInteger mixerVolume = totalVolume(throughMixers);
        List<String> services = throughMixers.stream()
                .map(party -> byAddress.get(party.address()).getName())
                .distinct()
                .toList();

        return new MixerExposure(
                services,
                labels.percent(mixerVolume, totalVolume(transaction.parties())),
                mixerVolume.toString());
    }

    private TransactionHeuristics heuristics(TransactionSnapshot transaction, Chain chain) {
        BigInteger value = new BigInteger(transaction.valueNative());
        boolean tokenOnly = value.signum() == 0 && transaction.tokenTransferCount() > 0;

        return new TransactionHeuristics(
                !transaction.success(),
                value.signum() == 0 && transaction.tokenTransferCount() == 0,
                labels.isRoundAmount(value, chain),
                selfTransfer(transaction),
                tokenOnly,
                transaction.nestedTransferCount() >= properties.internalFanOutThreshold(),
                (int) transaction.parties().stream().map(TransactionParty::address).distinct().count()
        );
    }

    private boolean selfTransfer(TransactionSnapshot transaction) {
        return transaction.fromAddress() != null
                && transaction.fromAddress().equals(transaction.toAddress());
    }

    private TransferDirection direction(TransactionParty party) {
        return SENDING_ROLES.contains(party.role()) ? TransferDirection.OUT : TransferDirection.IN;
    }

    private int hops(TransactionParty party) {
        return DIRECT_ROLES.contains(party.role()) ? DIRECT_HOPS : NESTED_HOPS;
    }

    private BigInteger totalVolume(List<TransactionParty> parties) {
        return parties.stream()
                .map(party -> new BigInteger(party.valueNative()))
                .reduce(BigInteger.ZERO, BigInteger::add);
    }
}
