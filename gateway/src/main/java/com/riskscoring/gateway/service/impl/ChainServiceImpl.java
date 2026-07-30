package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.EvmChain;
import com.riskscoring.gateway.dto.ChainCandidatesResponse;
import com.riskscoring.gateway.exception.UnrecognizedAddressException;
import com.riskscoring.gateway.model.EvmAddresses;
import com.riskscoring.gateway.service.ChainService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChainServiceImpl implements ChainService {

    private static final List<Integer> EVM_CHAIN_IDS = EvmChain.mainnets().stream()
            .map(EvmChain::chainId)
            .toList();

    @Override
    public ChainCandidatesResponse candidatesFor(String address) {
        if (!EvmAddresses.isValid(address)) {
            throw new UnrecognizedAddressException(address);
        }

        return new ChainCandidatesResponse(EvmAddresses.normalize(address), EVM_CHAIN_IDS);
    }
}
