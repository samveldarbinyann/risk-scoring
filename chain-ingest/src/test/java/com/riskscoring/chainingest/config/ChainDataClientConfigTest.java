package com.riskscoring.chainingest.config;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.ChainDataClientKey;
import com.riskscoring.chainingest.service.ChainFactsCacheService;
import com.riskscoring.common.model.ChainFamily;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChainDataClientConfigTest {

    @Mock
    private ChainDataClient evmAddressClient;

    @Mock
    private ChainDataClient bitcoinAddressClient;

    @Mock
    private ChainDataClient duplicateEvmAddressClient;

    @Mock
    private ChainFactsCacheService addressCache;

    @Mock
    private ChainFactsCacheService duplicateAddressCache;

    private final ChainDataClientConfig config = new ChainDataClientConfig();

    @Test
    void chainDataClientsMapsDistinctFamilyTargetPairs() {
        lenient().when(evmAddressClient.family()).thenReturn(ChainFamily.EVM);
        lenient().when(evmAddressClient.target()).thenReturn(ScanTarget.ADDRESS);
        lenient().when(bitcoinAddressClient.family()).thenReturn(ChainFamily.BITCOIN);
        lenient().when(bitcoinAddressClient.target()).thenReturn(ScanTarget.ADDRESS);

        Map<ChainDataClientKey, ChainDataClient> result =
                config.chainDataClients(List.of(evmAddressClient, bitcoinAddressClient));

        assertThat(result).containsEntry(new ChainDataClientKey(ChainFamily.EVM, ScanTarget.ADDRESS), evmAddressClient);
        assertThat(result).containsEntry(new ChainDataClientKey(ChainFamily.BITCOIN, ScanTarget.ADDRESS), bitcoinAddressClient);
    }

    @Test
    void chainDataClientsThrowsOnDuplicateFamilyTargetPair() {
        when(evmAddressClient.family()).thenReturn(ChainFamily.EVM);
        when(evmAddressClient.target()).thenReturn(ScanTarget.ADDRESS);
        when(duplicateEvmAddressClient.family()).thenReturn(ChainFamily.EVM);
        when(duplicateEvmAddressClient.target()).thenReturn(ScanTarget.ADDRESS);

        assertThatThrownBy(() -> config.chainDataClients(List.of(evmAddressClient, duplicateEvmAddressClient)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void chainDataClientsMapIsImmutable() {
        when(evmAddressClient.family()).thenReturn(ChainFamily.EVM);
        when(evmAddressClient.target()).thenReturn(ScanTarget.ADDRESS);

        Map<ChainDataClientKey, ChainDataClient> result = config.chainDataClients(List.of(evmAddressClient));

        assertThatThrownBy(() -> result.put(new ChainDataClientKey(ChainFamily.TRON, ScanTarget.ADDRESS), evmAddressClient))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void chainFactsCachesMapsDistinctTargets() {
        when(addressCache.target()).thenReturn(ScanTarget.ADDRESS);
        when(duplicateAddressCache.target()).thenReturn(ScanTarget.TRANSACTION);

        Map<ScanTarget, ChainFactsCacheService> result =
                config.chainFactsCaches(List.of(addressCache, duplicateAddressCache));

        assertThat(result).containsEntry(ScanTarget.ADDRESS, addressCache);
        assertThat(result).containsEntry(ScanTarget.TRANSACTION, duplicateAddressCache);
    }

    @Test
    void chainFactsCachesThrowsOnDuplicateTarget() {
        when(addressCache.target()).thenReturn(ScanTarget.ADDRESS);
        when(duplicateAddressCache.target()).thenReturn(ScanTarget.ADDRESS);

        assertThatThrownBy(() -> config.chainFactsCaches(List.of(addressCache, duplicateAddressCache)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void chainFactsCachesMapIsImmutable() {
        when(addressCache.target()).thenReturn(ScanTarget.ADDRESS);

        Map<ScanTarget, ChainFactsCacheService> result = config.chainFactsCaches(List.of(addressCache));

        assertThatThrownBy(() -> result.put(ScanTarget.TRANSACTION, addressCache))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
