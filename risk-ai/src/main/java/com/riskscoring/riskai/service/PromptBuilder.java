package com.riskscoring.riskai.service;

import com.riskscoring.common.model.EvidenceBundle;

public interface PromptBuilder {

    String systemPrompt();

    String userPrompt(EvidenceBundle evidence);

    String retryPrompt(EvidenceBundle evidence, String invalidResponse, String reason);
}
