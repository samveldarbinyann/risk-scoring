package com.riskscoring.chainingest.client;

import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChainDataClientKeyTest {

    @Mock
    private ChainDataClient client;

    @Test
    void ofBuildsKeyFromClientFamilyAndTarget() {
        when(client.family()).thenReturn(ChainFamily.EVM);
        when(client.target()).thenReturn(ScanTarget.ADDRESS);

        ChainDataClientKey key = ChainDataClientKey.of(client);

        assertThat(key).isEqualTo(new ChainDataClientKey(ChainFamily.EVM, ScanTarget.ADDRESS));
    }

    @Test
    void keysWithSameFamilyAndTargetAreEqual() {
        ChainDataClientKey a = new ChainDataClientKey(ChainFamily.BITCOIN, ScanTarget.TRANSACTION);
        ChainDataClientKey b = new ChainDataClientKey(ChainFamily.BITCOIN, ScanTarget.TRANSACTION);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
