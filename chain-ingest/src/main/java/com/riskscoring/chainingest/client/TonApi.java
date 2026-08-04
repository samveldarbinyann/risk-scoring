package com.riskscoring.chainingest.client;

import com.riskscoring.chainingest.client.dto.tonapi.TonAccount;
import com.riskscoring.chainingest.client.dto.tonapi.TonEvent;
import com.riskscoring.chainingest.client.dto.tonapi.TonJettonBalance;

import java.util.List;
import java.util.Optional;

public interface TonApi {

    TonAccount account(String address);

    List<TonEvent> accountEvents(String address);

    Optional<TonEvent> firstEvent(String address);

    List<TonJettonBalance> jettons(String address);

    TonEvent event(String hash);
}
