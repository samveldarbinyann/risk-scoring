package com.riskscoring.gateway.websocket;

import com.riskscoring.gateway.dto.ScanProgressMessage;

public interface ScanNotifier {

    void notifyProgress(ScanProgressMessage message);
}