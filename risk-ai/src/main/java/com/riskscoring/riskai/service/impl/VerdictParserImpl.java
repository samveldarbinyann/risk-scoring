package com.riskscoring.riskai.service.impl;

import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.Verdict;
import com.riskscoring.riskai.exception.InvalidVerdictException;
import com.riskscoring.riskai.service.VerdictParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VerdictParserImpl implements VerdictParser {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    private static final Map<RiskLevel, int[]> SCORE_RANGES = Map.of(
            RiskLevel.LOW, new int[]{0, 25},
            RiskLevel.MEDIUM, new int[]{26, 50},
            RiskLevel.HIGH, new int[]{51, 80},
            RiskLevel.CRITICAL, new int[]{81, 100}
    );

    private final ObjectMapper objectMapper;

    @Override
    public Verdict parse(String llmResponse) {
        Verdict verdict = readJson(llmResponse);

        if (verdict.riskLevel() == null) {
            throw new InvalidVerdictException("riskLevel is missing");
        }
        if (verdict.score() < MIN_SCORE || verdict.score() > MAX_SCORE) {
            throw new InvalidVerdictException("score %d is outside 0-100".formatted(verdict.score()));
        }
        if (!StringUtils.hasText(verdict.explanation())) {
            throw new InvalidVerdictException("explanation is empty");
        }

        int[] range = SCORE_RANGES.get(verdict.riskLevel());
        if (verdict.score() < range[0] || verdict.score() > range[1]) {
            throw new InvalidVerdictException(
                    "score %d does not match risk level %s (expected %d-%d)"
                            .formatted(verdict.score(), verdict.riskLevel(), range[0], range[1]));
        }

        return new Verdict(
                verdict.riskLevel(),
                verdict.score(),
                verdict.explanation(),
                orEmpty(verdict.decisiveSignals()),
                orEmpty(verdict.manualChecks())
        );
    }

    private Verdict readJson(String llmResponse) {
        try {
            return objectMapper.readValue(llmResponse, Verdict.class);
        } catch (JacksonException exception) {
            throw new InvalidVerdictException("response is not valid JSON: " + exception.getOriginalMessage(), exception);
        }
    }

    private List<String> orEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }
}
