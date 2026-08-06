package com.riskscoring.gateway.kafka;

import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.service.ScanProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScanProgressListenerTest {

    @Mock
    private ScanProgressService scanProgressService;

    private ScanProgressListener listener;

    @BeforeEach
    void setUp() {
        listener = new ScanProgressListener(scanProgressService);
    }

    @Test
    void onScanProgressDelegatesToScanProgressService() {
        ScanProgress event = new ScanProgress(UUID.randomUUID(), ScanStage.ANALYZING,
                "console.message.analyzing", List.of(), Language.EN, Instant.now());

        listener.onScanProgress(event);

        verify(scanProgressService).applyProgress(event);
    }
}
