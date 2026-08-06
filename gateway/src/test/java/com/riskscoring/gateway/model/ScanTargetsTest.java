package com.riskscoring.gateway.model;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ChainSupport;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.exception.TargetChainMismatchException;
import com.riskscoring.gateway.exception.UnrecognizedTargetException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScanTargetsTest {

    @Test
    void classifyMatchesEveryEvmChainForAnEvmAddress() {
        String address = "0x" + "a".repeat(40);

        List<TargetMatch> matches = ScanTargets.classify(address);

        assertThat(matches).hasSize(Chain.of(ChainFamily.EVM).size());
        assertThat(matches).allSatisfy(match -> {
            assertThat(match.targetType()).isEqualTo(ScanTarget.ADDRESS);
            assertThat(match.normalizedTarget()).isEqualTo(address);
        });
    }

    @Test
    void classifyThrowsUnrecognizedTargetExceptionForGarbageInput() {
        assertThatThrownBy(() -> ScanTargets.classify("not-a-valid-target"))
                .isInstanceOf(UnrecognizedTargetException.class);
    }

    @Test
    void classifySortsSupportedChainsBeforePlannedChainsOnFormatCollision() {
        // 0x + 64 hex chars matches EVM's transaction pattern AND SUI's address pattern.
        String value = "0x" + "a".repeat(64);

        List<TargetMatch> matches = ScanTargets.classify(value);

        assertThat(matches).isNotEmpty();
        assertThat(matches.getLast().chain()).isEqualTo(Chain.SUI);
        assertThat(matches.getLast().chain().support()).isEqualTo(ChainSupport.PLANNED);
        assertThat(matches.subList(0, matches.size() - 1))
                .allSatisfy(match -> assertThat(match.chain().support()).isEqualTo(ChainSupport.SUPPORTED));
    }

    @Test
    void requireReturnsNormalizedMatchWhenTargetMatchesChainAndExpectedType() {
        String mixedCase = "0x" + "A".repeat(40);

        TargetMatch match = ScanTargets.require(mixedCase, Chain.ETHEREUM, ScanTarget.ADDRESS);

        assertThat(match.chain()).isEqualTo(Chain.ETHEREUM);
        assertThat(match.targetType()).isEqualTo(ScanTarget.ADDRESS);
        assertThat(match.normalizedTarget()).isEqualTo("0x" + "a".repeat(40));
    }

    @Test
    void requireThrowsTargetChainMismatchExceptionWhenTargetTypeDiffersFromExpected() {
        String address = "0x" + "a".repeat(40);

        assertThatThrownBy(() -> ScanTargets.require(address, Chain.ETHEREUM, ScanTarget.TRANSACTION))
                .isInstanceOf(TargetChainMismatchException.class);
    }

    @Test
    void requireThrowsTargetChainMismatchExceptionWhenValueDoesNotMatchChainFamilyFormat() {
        assertThatThrownBy(() -> ScanTargets.require("not-a-valid-target", Chain.ETHEREUM, ScanTarget.ADDRESS))
                .isInstanceOf(TargetChainMismatchException.class);
    }
}
