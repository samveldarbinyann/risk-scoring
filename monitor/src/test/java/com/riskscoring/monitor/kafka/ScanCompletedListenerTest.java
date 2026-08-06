package com.riskscoring.monitor.kafka;

import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.Verdict;
import com.riskscoring.monitor.service.RecheckService;
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
class ScanCompletedListenerTest {

    @Mock
    private RecheckService recheckService;

    private ScanCompletedListener listener;

    @BeforeEach
    void setUp() {
        listener = new ScanCompletedListener(recheckService);
    }

    @Test
    void onScanCompletedDelegatesToRecheckService() {
        Verdict verdict = new Verdict(RiskLevel.LOW, 10, "clean wallet", List.of(), List.of());
        ScanCompleted event = new ScanCompleted(
                UUID.randomUUID(), ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM, verdict, "deepseek-chat", Instant.now());

        listener.onScanCompleted(event);

        verify(recheckService).handleScanCompleted(event);
    }
}
