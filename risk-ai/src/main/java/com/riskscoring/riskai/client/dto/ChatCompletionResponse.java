package com.riskscoring.riskai.client.dto;

import java.util.List;

public record ChatCompletionResponse(
        String id,
        String model,
        List<Choice> choices,
        Usage usage
) {
    public record Choice(
            Integer index,
            ChatMessage message,
            String finishReason
    ) {
    }

    public record Usage(
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
    }
}
