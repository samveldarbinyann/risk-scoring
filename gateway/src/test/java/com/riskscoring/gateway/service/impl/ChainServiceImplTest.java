package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.gateway.dto.ChainCandidatesResponse;
import com.riskscoring.gateway.dto.ChainView;
import com.riskscoring.gateway.exception.ChainNotSupportedYetException;
import com.riskscoring.gateway.exception.UnrecognizedTargetException;
import com.riskscoring.gateway.exception.UnsupportedChainException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChainServiceImplTest {

    private final ChainServiceImpl service = new ChainServiceImpl();

    @Test
    void registryContainsOneEntryPerChain() {
        List<ChainView> registry = service.registry();

        assertThat(registry).hasSize(Chain.values().length);
        assertThat(registry).extracting(ChainView::chain).contains(Chain.ETHEREUM, Chain.BITCOIN, Chain.SUI);
    }

    @Test
    void registryReflectsEvmChainIdOnlyForEvmChains() {
        List<ChainView> registry = service.registry();

        ChainView ethereum = registry.stream().filter(view -> view.chain() == Chain.ETHEREUM).findFirst().orElseThrow();
        ChainView bitcoin = registry.stream().filter(view -> view.chain() == Chain.BITCOIN).findFirst().orElseThrow();

        assertThat(ethereum.evmChainId()).isNotNull();
        assertThat(bitcoin.evmChainId()).isNull();
    }

    @Test
    void candidatesForReturnsNormalizedTargetAndMatchesForAValidAddress() {
        String address = "0x" + "a".repeat(40);

        ChainCandidatesResponse response = service.candidatesFor(address);

        assertThat(response.target()).isEqualTo(address);
        assertThat(response.candidates()).isNotEmpty();
    }

    @Test
    void candidatesForThrowsUnrecognizedTargetExceptionForGarbageInput() {
        assertThatThrownBy(() -> service.candidatesFor("not-a-valid-target"))
                .isInstanceOf(UnrecognizedTargetException.class);
    }

    @Test
    void requireScannableReturnsChainForSupportedKey() {
        assertThat(service.requireScannable("ETHEREUM")).isEqualTo(Chain.ETHEREUM);
    }

    @Test
    void requireScannableThrowsUnsupportedChainExceptionForUnknownKey() {
        assertThatThrownBy(() -> service.requireScannable("NOT_A_CHAIN"))
                .isInstanceOf(UnsupportedChainException.class);
    }

    @Test
    void requireScannableThrowsChainNotSupportedYetExceptionForPlannedChain() {
        assertThatThrownBy(() -> service.requireScannable("SUI"))
                .isInstanceOf(ChainNotSupportedYetException.class);
    }
}
