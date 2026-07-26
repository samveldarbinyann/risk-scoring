package com.riskscoring.enrichment.service;

import com.riskscoring.common.event.ChainFetched;

public interface EnrichmentService {

    void enrich(ChainFetched event);
}