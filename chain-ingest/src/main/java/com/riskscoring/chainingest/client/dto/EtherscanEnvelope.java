package com.riskscoring.chainingest.client.dto;

import tools.jackson.databind.JsonNode;

public record EtherscanEnvelope(
        String status,
        String message,
        JsonNode result
) {
}
