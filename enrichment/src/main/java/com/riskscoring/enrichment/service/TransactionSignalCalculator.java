package com.riskscoring.enrichment.service;

import com.riskscoring.common.event.ChainFetched;
import com.riskscoring.common.model.TransactionEvidence;
import com.riskscoring.common.model.TransactionFacts;
import com.riskscoring.enrichment.entity.Label;

import java.util.Map;

public interface TransactionSignalCalculator {

    TransactionEvidence calculate(ChainFetched event, TransactionFacts facts, Map<String, Label> labelsByAddress);
}
