package com.riskscoring.riskai.service;

import com.riskscoring.common.event.SignalsComputed;

public interface RiskAiService {

    void analyze(SignalsComputed event);
}
