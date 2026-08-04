package com.riskscoring.chainingest.client.dto.tonapi;

import java.util.List;

public record TonEvents(
        List<TonEvent> events
) {
}
