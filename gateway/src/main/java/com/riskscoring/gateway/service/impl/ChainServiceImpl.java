package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.EvmChain;
import com.riskscoring.gateway.dto.ChainCandidateView;
import com.riskscoring.gateway.dto.ChainCandidatesResponse;
import com.riskscoring.gateway.exception.UnrecognizedAddressException;
import com.riskscoring.gateway.model.AddressFamily;
import com.riskscoring.gateway.service.ChainService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChainServiceImpl implements ChainService {

    @Override
    public ChainCandidatesResponse candidatesFor(String address) {
        String trimmed = address == null ? "" : address.trim();

        AddressFamily family = AddressFamily.detect(trimmed)
                .orElseThrow(() -> new UnrecognizedAddressException(trimmed));

        String normalized = family.normalize(trimmed);

        return new ChainCandidatesResponse(normalized, family, candidateChains(family));
    }

    private List<ChainCandidateView> candidateChains(AddressFamily family) {
        return switch (family) {
            case EVM -> EvmChain.mainnets().stream()
                    .map(chain -> new ChainCandidateView(chain.chainId(), chain.displayName()))
                    .toList();
        };
    }
}
