package com.riskscoring.chainingest.client.dto.tonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TonEvent(
        @JsonProperty("event_id") String eventId,
        Long timestamp,
        @JsonProperty("is_scam") boolean scam,
        List<TonAction> actions
) {
}
