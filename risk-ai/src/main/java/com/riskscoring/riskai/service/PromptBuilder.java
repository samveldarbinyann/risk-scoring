package com.riskscoring.riskai.service;

import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Language;

public interface PromptBuilder {

    String systemPrompt(EvidenceBundle evidence, Language language);

    String userPrompt(EvidenceBundle evidence);

    String retryPrompt(EvidenceBundle evidence, String invalidResponse, String reason);
}
