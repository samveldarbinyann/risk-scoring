package com.riskscoring.enrichment.service;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.AddressFacts;
import com.riskscoring.enrichment.entity.Label;

import java.util.Map;

public interface AddressSignalCalculator {

    AddressEvidence calculate(ChainFetched event, AddressFacts facts, Map<String, Label> labelsByAddress);
}
