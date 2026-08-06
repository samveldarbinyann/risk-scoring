package com.riskscoring.chainingest.kafka.impl;

import com.riskscoring.common.Topics;
import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.common.model.AddressSnapshot;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChainEventPublisherImplTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void publishChainFetchedSendsToChainFetchedTopicKeyedByScanId() {
        ChainEventPublisherImpl impl = new ChainEventPublisherImpl(kafkaTemplate);
        UUID scanId = UUID.randomUUID();
        ChainFetched event = new ChainFetched(scanId, ScanTarget.ADDRESS, "0xabc", Chain.ETHEREUM,
                new AddressFacts(new AddressSnapshot(0, 0, "0", List.of(), null, null, false, Instant.now()), List.of()),
                Language.EN, Instant.now());

        impl.publishChainFetched(event);

        verify(kafkaTemplate).send(Topics.CHAIN_FETCHED, scanId.toString(), event);
    }

    @Test
    void publishScanProgressSendsToScanProgressTopicKeyedByScanId() {
        ChainEventPublisherImpl impl = new ChainEventPublisherImpl(kafkaTemplate);
        UUID scanId = UUID.randomUUID();
        ScanProgress event = new ScanProgress(scanId, ScanStage.FETCHING, "key", List.of(), Language.EN, Instant.now());

        impl.publishScanProgress(event);

        verify(kafkaTemplate).send(Topics.SCAN_PROGRESS, scanId.toString(), event);
    }
}
