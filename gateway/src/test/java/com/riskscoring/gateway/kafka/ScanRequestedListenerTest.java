package com.riskscoring.gateway.kafka;

import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.service.ScanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ScanRequestedListenerTest {

    @Mock
    private ScanService scanService;

    private ScanRequestedListener listener;

    @BeforeEach
    void setUp() {
        listener = new ScanRequestedListener(scanService);
    }

    @Test
    void onScanRequestedIngestsEventWhenSourceIsMonitor() {
        ScanRequested event = event(ScanSource.MONITOR);

        listener.onScanRequested(event);

        verify(scanService).ingestMonitorScan(event);
    }

    @Test
    void onScanRequestedIgnoresEventWhenSourceIsNotMonitor() {
        ScanRequested event = event(ScanSource.API);

        listener.onScanRequested(event);

        verifyNoInteractions(scanService);
    }

    private static ScanRequested event(ScanSource source) {
        return new ScanRequested(UUID.randomUUID(), ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM,
                Instant.now(), source, Language.EN, UUID.randomUUID());
    }
}
