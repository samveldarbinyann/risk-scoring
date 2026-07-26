package com.riskscoring.enrichment.service;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.enrichment.entity.Label;

import java.util.Map;

public interface RiskSignalCalculator {

    EvidenceBundle calculate(ChainFetched event, Map<String, Label> labelsByAddress);
}