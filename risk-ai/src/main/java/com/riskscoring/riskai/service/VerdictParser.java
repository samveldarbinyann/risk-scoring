package com.riskscoring.riskai.service;

import com.riskscoring.common.model.Verdict;

public interface VerdictParser {

    Verdict parse(String llmResponse);
}
