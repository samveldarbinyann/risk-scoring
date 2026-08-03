package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.EvmChain;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.dto.ChainCandidatesResponse;
import com.riskscoring.gateway.model.ScanTargets;
import com.riskscoring.gateway.service.ChainService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChainServiceImpl implements ChainService {

    private static final List<Integer> EVM_CHAIN_IDS = EvmChain.mainnets().stream()
            .map(EvmChain::chainId)
            .toList();

    @Override
    public ChainCandidatesResponse candidatesFor(String target) {
        ScanTarget targetType = ScanTargets.classify(target);

        return new ChainCandidatesResponse(targetType, ScanTargets.normalize(target), EVM_CHAIN_IDS);
    }
}
