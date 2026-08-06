package com.riskscoring.chainingest.exception;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnsupportedChainExceptionTest {

    @Test
    void addressTargetUsesAddressMessageKey() {
        UnsupportedChainException exception = new UnsupportedChainException(Chain.SUI, ScanTarget.ADDRESS);

        assertThat(exception.progressMessageKey()).isEqualTo("console.message.unsupportedChainAddress");
        assertThat(exception.getMessage()).isEqualTo("Sui address scans are not supported yet");
    }

    @Test
    void transactionTargetUsesTransactionMessageKey() {
        UnsupportedChainException exception = new UnsupportedChainException(Chain.SUI, ScanTarget.TRANSACTION);

        assertThat(exception.progressMessageKey()).isEqualTo("console.message.unsupportedChainTransaction");
        assertThat(exception.getMessage()).isEqualTo("Sui transaction scans are not supported yet");
    }

    @Test
    void messageArgsContainChainDisplayName() {
        UnsupportedChainException exception = new UnsupportedChainException(Chain.BITCOIN, ScanTarget.ADDRESS);

        assertThat(exception.progressMessageArgs()).isEqualTo(List.of("Bitcoin"));
    }
}
