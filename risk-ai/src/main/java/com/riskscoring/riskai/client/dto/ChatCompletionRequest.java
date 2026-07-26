package com.riskscoring.riskai.client.dto;

import java.util.List;

public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        ResponseFormat responseFormat,
        double temperature,
        boolean stream
) {
    public record ResponseFormat(String type) {

        public static ResponseFormat jsonObject() {
            return new ResponseFormat("json_object");
        }
    }
}
