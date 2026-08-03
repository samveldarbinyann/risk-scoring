package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.gateway.dto.ChainCandidate;
import com.riskscoring.gateway.dto.ChainCandidatesResponse;
import com.riskscoring.gateway.dto.ChainView;
import com.riskscoring.gateway.exception.ChainNotSupportedYetException;
import com.riskscoring.gateway.exception.UnsupportedChainException;
import com.riskscoring.gateway.model.ScanTargets;
import com.riskscoring.gateway.model.TargetMatch;
import com.riskscoring.gateway.service.ChainService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ChainServiceImpl implements ChainService {

    private static final List<ChainView> REGISTRY = Arrays.stream(Chain.values())
            .map(chain -> new ChainView(
                    chain,
                    chain.family(),
                    chain.displayName(),
                    chain.nativeSymbol(),
                    chain.nativeDecimals(),
                    chain.evmChainId().isPresent() ? chain.evmChainId().getAsInt() : null,
                    chain.mainnet(),
                    chain.support()))
            .toList();

    @Override
    public List<ChainView> registry() {
        return REGISTRY;
    }

    @Override
    public ChainCandidatesResponse candidatesFor(String target) {
        List<TargetMatch> matches = ScanTargets.classify(target);

        return new ChainCandidatesResponse(
                matches.getFirst().normalizedTarget(),
                matches.stream().map(ChainServiceImpl::toCandidate).toList());
    }

    @Override
    public Chain requireScannable(String key) {
        Chain chain = Chain.byKey(key).orElseThrow(() -> new UnsupportedChainException(key));

        if (!chain.scannable()) {
            throw new ChainNotSupportedYetException(chain);
        }

        return chain;
    }

    private static ChainCandidate toCandidate(TargetMatch match) {
        Chain chain = match.chain();

        return new ChainCandidate(
                chain,
                chain.family(),
                chain.displayName(),
                chain.nativeSymbol(),
                match.targetType(),
                chain.support(),
                match.normalizedTarget());
    }
}
