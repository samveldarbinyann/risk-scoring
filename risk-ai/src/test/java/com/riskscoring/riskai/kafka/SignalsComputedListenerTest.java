package com.riskscoring.riskai.kafka;

import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.riskai.service.RiskAiService;
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
class SignalsComputedListenerTest {

    @Mock
    private RiskAiService riskAiService;

    private SignalsComputedListener listener;

    @BeforeEach
    void setUp() {
        listener = new SignalsComputedListener(riskAiService);
    }

    @Test
    void onSignalsComputedDelegatesToRiskAiService() {
        AddressEvidence evidence = new AddressEvidence("0xtarget", Chain.ETHEREUM, Instant.now(), null, 0, 0,
                false, "0", List.of(), 0, List.of(), null, new Heuristics(null, null, false, 0, 0));
        SignalsComputed event = new SignalsComputed(
                UUID.randomUUID(), ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM, evidence, Language.EN, Instant.now());

        listener.onSignalsComputed(event);

        verify(riskAiService).analyze(event);
    }
}
