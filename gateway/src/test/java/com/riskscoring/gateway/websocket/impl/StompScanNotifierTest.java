package com.riskscoring.gateway.websocket.impl;

import com.riskscoring.common.event.ScanStage;
import com.riskscoring.gateway.config.WebSocketConfig;
import com.riskscoring.gateway.dto.ScanProgressMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StompScanNotifierTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private StompScanNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new StompScanNotifier(messagingTemplate);
    }

    @Test
    void notifyProgressSendsToScanTopicForTheMessageScanId() {
        UUID scanId = UUID.randomUUID();
        ScanProgressMessage message = new ScanProgressMessage(scanId, ScanStage.ANALYZING, "analyzing", Instant.now());

        notifier.notifyProgress(message);

        verify(messagingTemplate).convertAndSend(WebSocketConfig.SCAN_TOPIC_PREFIX + scanId, message);
    }

    @Test
    void notifyGroupProgressSendsToScanGroupTopicForTheGivenGroupId() {
        UUID groupId = UUID.randomUUID();
        ScanProgressMessage message = new ScanProgressMessage(UUID.randomUUID(), ScanStage.COMPLETED, "done", Instant.now());

        notifier.notifyGroupProgress(groupId, message);

        verify(messagingTemplate).convertAndSend(WebSocketConfig.SCAN_GROUP_TOPIC_PREFIX + groupId, message);
    }
}
