package com.riskscoring.riskai.mapper;

import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.Verdict;
import com.riskscoring.riskai.entity.ScanReport;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScanReportMapperTest {

    private static final Instant OBSERVED_AT = Instant.parse("2024-06-01T00:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2024-06-01T00:05:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScanReportMapper mapper = new ScanReportMapper(objectMapper);

    @Test
    void toEntityMapsIdentityFieldsFromEvent() {
        SignalsComputed event = signalsComputed();
        Verdict verdict = verdict();

        ScanReport report = mapper.toEntity(event, verdict, "deepseek-chat", "v1", CREATED_AT);

        assertThat(report.getId()).isNotNull();
        assertThat(report.getScanId()).isEqualTo(event.scanId());
        assertThat(report.getTargetType()).isEqualTo(event.targetType());
        assertThat(report.getTarget()).isEqualTo(event.target());
        assertThat(report.getChain()).isEqualTo(event.chain());
    }

    @Test
    void toEntityMapsRiskFieldsFromVerdict() {
        SignalsComputed event = signalsComputed();
        Verdict verdict = verdict();

        ScanReport report = mapper.toEntity(event, verdict, "deepseek-chat", "v1", CREATED_AT);

        assertThat(report.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(report.getScore()).isEqualTo(70);
        assertThat(report.getExplanation()).isEqualTo("mixer exposure and sanctioned counterparty");
    }

    @Test
    void toEntitySerializesDecisiveSignalsAndManualChecksAsJsonLists() {
        ScanReport report = mapper.toEntity(signalsComputed(), verdict(), "deepseek-chat", "v1", CREATED_AT);

        assertThat(objectMapper.readValue(report.getDecisiveSignals(), String[].class))
                .containsExactly("mixer exposure 40%", "sanctioned counterparty at hop 1");
        assertThat(objectMapper.readValue(report.getManualChecks(), String[].class))
                .containsExactly("verify counterparty identity");
    }

    @Test
    void toEntityUsesEvidenceObservedAtAndSerializesEvidenceAsJson() {
        SignalsComputed event = signalsComputed();

        ScanReport report = mapper.toEntity(event, verdict(), "deepseek-chat", "v1", CREATED_AT);

        assertThat(report.getObservedAt()).isEqualTo(OBSERVED_AT);
        assertThat(objectMapper.readValue(report.getEvidence(), EvidenceBundle.class)).isEqualTo(event.evidence());
    }

    @Test
    void toEntityMapsModelPromptVersionAndCreatedAtFromArguments() {
        ScanReport report = mapper.toEntity(signalsComputed(), verdict(), "deepseek-chat", "v1", CREATED_AT);

        assertThat(report.getModel()).isEqualTo("deepseek-chat");
        assertThat(report.getPromptVersion()).isEqualTo("v1");
        assertThat(report.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void toEntityGeneratesDistinctRandomIdsAcrossCalls() {
        ScanReport first = mapper.toEntity(signalsComputed(), verdict(), "deepseek-chat", "v1", CREATED_AT);
        ScanReport second = mapper.toEntity(signalsComputed(), verdict(), "deepseek-chat", "v1", CREATED_AT);

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }

    @Test
    void toVerdictRoundTripsAllVerdictFieldsFromEntity() {
        ScanReport report = mapper.toEntity(signalsComputed(), verdict(), "deepseek-chat", "v1", CREATED_AT);

        Verdict roundTripped = mapper.toVerdict(report);

        assertThat(roundTripped).isEqualTo(verdict());
    }

    private static SignalsComputed signalsComputed() {
        AddressEvidence evidence = new AddressEvidence("0xtarget", Chain.ETHEREUM, OBSERVED_AT, null, 5, 1,
                false, "0", List.of(), 1, List.of(), null, new Heuristics(null, null, false, 1, 0));
        return new SignalsComputed(
                UUID.randomUUID(), ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM, evidence, Language.EN, OBSERVED_AT);
    }

    private static Verdict verdict() {
        return new Verdict(RiskLevel.HIGH, 70, "mixer exposure and sanctioned counterparty",
                List.of("mixer exposure 40%", "sanctioned counterparty at hop 1"),
                List.of("verify counterparty identity"));
    }
}
