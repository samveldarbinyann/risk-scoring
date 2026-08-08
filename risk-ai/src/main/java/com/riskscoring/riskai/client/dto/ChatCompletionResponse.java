package com.riskscoring.riskai.client.dto;

import java.util.List;

public record ChatCompletionResponse(List<Choice> choices) {

    public record Choice(ChatMessage message) {
    }
}
