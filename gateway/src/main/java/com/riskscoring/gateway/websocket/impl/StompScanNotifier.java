package com.riskscoring.gateway.websocket.impl;

import com.riskscoring.gateway.config.WebSocketConfig;
import com.riskscoring.gateway.dto.ScanProgressMessage;
import com.riskscoring.gateway.websocket.ScanNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompScanNotifier implements ScanNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyProgress(ScanProgressMessage message) {
        String destination = WebSocketConfig.SCAN_TOPIC_PREFIX + message.scanId();
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Pushed stage={} to {}", message.stage(), destination);
    }
}