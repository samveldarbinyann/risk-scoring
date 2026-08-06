package com.riskscoring.riskai.service.impl;

import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.Verdict;
import com.riskscoring.riskai.exception.InvalidVerdictException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerdictParserImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VerdictParserImpl parser = new VerdictParserImpl(objectMapper);

    @Test
    void parseReturnsVerdictForValidJson() {
        String json = """
                {"riskLevel":"HIGH","score":70,"explanation":"mixer exposure",
                 "decisiveSignals":["mixer 40%"],"manualChecks":["verify counterparty"]}
                """;

        Verdict verdict = parser.parse(json);

        assertThat(verdict).isEqualTo(new Verdict(RiskLevel.HIGH, 70, "mixer exposure",
                List.of("mixer 40%"), List.of("verify counterparty")));
    }

    @Test
    void parseAcceptsScoreAtLowUpperBoundary() {
        Verdict verdict = parser.parse(verdictJson("LOW", 25));

        assertThat(verdict.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(verdict.score()).isEqualTo(25);
    }

    @Test
    void parseAcceptsScoreAtMediumLowerBoundary() {
        Verdict verdict = parser.parse(verdictJson("MEDIUM", 26));

        assertThat(verdict.score()).isEqualTo(26);
    }

    @Test
    void parseAcceptsScoreAtCriticalUpperBoundary() {
        Verdict verdict = parser.parse(verdictJson("CRITICAL", 100));

        assertThat(verdict.score()).isEqualTo(100);
    }

    @Test
    void parseThrowsInvalidVerdictExceptionWhenRiskLevelIsMissing() {
        String json = """
                {"score":10,"explanation":"clean wallet"}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("riskLevel is missing");
    }

    @Test
    void parseThrowsInvalidVerdictExceptionWhenScoreBelowZero() {
        String json = """
                {"riskLevel":"LOW","score":-5,"explanation":"clean wallet"}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("outside 0-100");
    }

    @Test
    void parseThrowsInvalidVerdictExceptionWhenScoreAboveHundred() {
        String json = """
                {"riskLevel":"CRITICAL","score":150,"explanation":"clean wallet"}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("outside 0-100");
    }

    @Test
    void parseThrowsInvalidVerdictExceptionWhenExplanationIsBlank() {
        String json = """
                {"riskLevel":"LOW","score":10,"explanation":"   "}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("explanation is empty");
    }

    @Test
    void parseThrowsInvalidVerdictExceptionWhenExplanationIsMissing() {
        String json = """
                {"riskLevel":"LOW","score":10}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("explanation is empty");
    }

    @Test
    void parseThrowsInvalidVerdictExceptionWhenScoreOutsideRiskLevelRange() {
        String json = """
                {"riskLevel":"LOW","score":50,"explanation":"clean wallet"}
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("does not match risk level LOW");
    }

    @Test
    void parseThrowsInvalidVerdictExceptionWhenResponseIsNotValidJson() {
        assertThatThrownBy(() -> parser.parse("not valid json at all"))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void parseDefaultsNullDecisiveSignalsAndManualChecksToEmptyList() {
        String json = """
                {"riskLevel":"LOW","score":10,"explanation":"clean wallet"}
                """;

        Verdict verdict = parser.parse(json);

        assertThat(verdict.decisiveSignals()).isEmpty();
        assertThat(verdict.manualChecks()).isEmpty();
    }

    @Test
    void parseKeepsProvidedDecisiveSignalsAndManualChecks() {
        String json = """
                {"riskLevel":"LOW","score":10,"explanation":"clean wallet",
                 "decisiveSignals":["old active wallet"],"manualChecks":["none"]}
                """;

        Verdict verdict = parser.parse(json);

        assertThat(verdict.decisiveSignals()).containsExactly("old active wallet");
        assertThat(verdict.manualChecks()).containsExactly("none");
    }

    private static String verdictJson(String riskLevel, int score) {
        return """
                {"riskLevel":"%s","score":%d,"explanation":"clean wallet"}
                """.formatted(riskLevel, score);
    }
}
