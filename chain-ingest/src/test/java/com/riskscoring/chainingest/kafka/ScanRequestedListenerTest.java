package com.riskscoring.chainingest.kafka;

import com.riskscoring.chainingest.service.ChainIngestService;
import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScanRequestedListenerTest {

    @Mock
    private ChainIngestService chainIngestService;

    @Test
    void onScanRequestedDelegatesToChainIngestService() {
        ScanRequestedListener listener = new ScanRequestedListener(chainIngestService);
        ScanRequested event = new ScanRequested(UUID.randomUUID(), ScanTarget.ADDRESS, "0xabc", Chain.ETHEREUM,
                Instant.now(), ScanSource.USER, Language.EN, UUID.randomUUID());

        listener.onScanRequested(event);

        verify(chainIngestService).ingest(event);
    }
}
