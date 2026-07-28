package com.riskscoring.gateway.websocket;

import com.riskscoring.gateway.dto.ScanProgressMessage;

import java.util.UUID;

public interface ScanNotifier {

    void notifyProgress(ScanProgressMessage message);

    void notifyGroupProgress(UUID groupId, ScanProgressMessage message);
}