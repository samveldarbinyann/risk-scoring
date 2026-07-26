package com.riskscoring.riskai.client;

public interface LlmClient {

    String complete(String systemPrompt, String userPrompt);

    String model();
}
