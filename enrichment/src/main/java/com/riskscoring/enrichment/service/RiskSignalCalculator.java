package com.riskscoring.enrichment.service;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.enrichment.entity.Label;

import java.util.Map;
import java.util.Set;

public interface RiskSignalCalculator {

    Set<String> addressesToLabel(ChainFetched event);

    EvidenceBundle calculate(ChainFetched event, Map<String, Label> labelsByAddress);
}
