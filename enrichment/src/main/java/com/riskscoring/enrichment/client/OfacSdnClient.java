package com.riskscoring.enrichment.client;

import com.riskscoring.enrichment.client.dto.OfacDigitalCurrencyAddress;

import java.util.List;

public interface OfacSdnClient {

    List<OfacDigitalCurrencyAddress> fetchDigitalCurrencyAddresses();
}
