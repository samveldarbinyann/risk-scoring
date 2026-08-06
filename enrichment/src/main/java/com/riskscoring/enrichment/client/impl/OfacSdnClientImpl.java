package com.riskscoring.enrichment.client.impl;

import com.riskscoring.enrichment.client.OfacSdnClient;
import com.riskscoring.enrichment.client.OfacSdnParser;
import com.riskscoring.enrichment.client.dto.OfacDigitalCurrencyAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OfacSdnClientImpl implements OfacSdnClient {

    private final RestClient ofacRestClient;
    private final OfacSdnParser parser;

    @Override
    public List<OfacDigitalCurrencyAddress> fetchDigitalCurrencyAddresses() {
        return ofacRestClient.get()
                .exchange((request, response) -> parser.parse(response.getBody()));
    }
}
