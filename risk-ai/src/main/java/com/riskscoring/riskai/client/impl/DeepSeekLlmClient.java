package com.riskscoring.riskai.client.impl;

import com.riskscoring.riskai.client.LlmClient;
import com.riskscoring.riskai.client.dto.ChatCompletionRequest;
import com.riskscoring.riskai.client.dto.ChatCompletionResponse;
import com.riskscoring.riskai.client.dto.ChatMessage;
import com.riskscoring.riskai.config.RiskAiProperties;
import com.riskscoring.riskai.exception.LlmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeepSeekLlmClient implements LlmClient {

    private static final String COMPLETIONS_PATH = "/chat/completions";

    private final RestClient llmRestClient;
    private final RiskAiProperties properties;

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.llm().model(),
                List.of(ChatMessage.system(systemPrompt), ChatMessage.user(userPrompt)),
                ChatCompletionRequest.ResponseFormat.jsonObject(),
                properties.llm().temperature(),
                false
        );

        ChatCompletionResponse response = llmRestClient.post()
                .uri(COMPLETIONS_PATH)
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);

        return Optional.ofNullable(response)
                .map(ChatCompletionResponse::choices)
                .filter(choices -> !choices.isEmpty())
                .map(choices -> choices.getFirst().message().content())
                .orElseThrow(() -> new LlmException("LLM returned no choices"));
    }

    @Override
    public String model() {
        return properties.llm().model();
    }
}
