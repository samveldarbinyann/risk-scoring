package com.riskscoring.enrichment.kafka;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.enrichment.service.EnrichmentService;
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
class ChainFetchedListenerTest {

    @Mock
    private EnrichmentService enrichmentService;

    private ChainFetchedListener listener;

    @BeforeEach
    void setUp() {
        listener = new ChainFetchedListener(enrichmentService);
    }

    @Test
    void onChainFetchedDelegatesToEnrichmentService() {
        Instant now = Instant.parse("2024-06-01T00:00:00Z");
        AddressFacts facts = new AddressFacts(
                new AddressSnapshot(1, 1, "0", List.of(), null, now, false, now), List.of());
        ChainFetched event = new ChainFetched(
                UUID.randomUUID(), ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM, facts, Language.EN, now);

        listener.onChainFetched(event);

        verify(enrichmentService).enrich(event);
    }
}
