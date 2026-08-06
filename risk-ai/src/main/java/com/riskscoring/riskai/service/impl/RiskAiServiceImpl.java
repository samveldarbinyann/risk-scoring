package com.riskscoring.riskai.service.impl;

import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.Verdict;
import com.riskscoring.riskai.client.LlmClient;
import com.riskscoring.riskai.config.RiskAiProperties;
import com.riskscoring.riskai.entity.ScanReport;
import com.riskscoring.riskai.exception.InvalidVerdictException;
import com.riskscoring.riskai.kafka.RiskAiEventPublisher;
import com.riskscoring.riskai.mapper.ScanReportMapper;
import com.riskscoring.riskai.repository.ScanReportRepository;
import com.riskscoring.riskai.service.PromptBuilder;
import com.riskscoring.riskai.service.RiskAiService;
import com.riskscoring.riskai.service.VerdictParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAiServiceImpl implements RiskAiService {

    private static final String PROGRESS_MESSAGE_KEY = "console.message.analyzing";

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final VerdictParser verdictParser;
    private final ScanReportRepository scanReportRepository;
    private final ScanReportMapper scanReportMapper;
    private final RiskAiEventPublisher eventPublisher;
    private final RiskAiProperties properties;

    @Override
    public void analyze(SignalsComputed event) {
        Optional<ScanReport> stored = scanReportRepository.findByScanId(event.scanId());
        if (stored.isPresent()) {
            ScanReport report = stored.get();
            log.info("Report for scanId={} already exists, republishing completion", event.scanId());
            publishCompletion(event, scanReportMapper.toVerdict(report), report.getModel(), report.getCreatedAt());
            return;
        }

        eventPublisher.publishScanProgress(
                new ScanProgress(event.scanId(), ScanStage.ANALYZING, PROGRESS_MESSAGE_KEY, List.of(), event.language(), Instant.now()));

        Verdict verdict = askForVerdict(event.evidence(), event.language());

        Instant completedAt = Instant.now();

        scanReportRepository.save(scanReportMapper.toEntity(
                event, verdict, llmClient.model(), properties.promptVersion(), completedAt));

        log.info("Verdict for scanId={} is {} score={}", event.scanId(), verdict.riskLevel(), verdict.score());

        publishCompletion(event, verdict, llmClient.model(), completedAt);
    }

    private void publishCompletion(SignalsComputed event, Verdict verdict, String model, Instant completedAt) {
        eventPublisher.publishScanCompleted(new ScanCompleted(
                event.scanId(),
                event.targetType(),
                event.target(),
                event.chain(),
                verdict,
                model,
                completedAt
        ));
        eventPublisher.publishScanProgress(
                new ScanProgress(event.scanId(), ScanStage.COMPLETED, "risk.level." + verdict.riskLevel().name(),
                        List.of(), event.language(), Instant.now()));
    }

    private Verdict askForVerdict(EvidenceBundle evidence, Language language) {
        String systemPrompt = promptBuilder.systemPrompt(evidence, language);
        String prompt = promptBuilder.userPrompt(evidence);
        InvalidVerdictException lastFailure = null;

        for (int attempt = 1; attempt <= properties.maxVerdictAttempts(); attempt++) {
            String response = llmClient.complete(systemPrompt, prompt);
            try {
                return verdictParser.parse(response);
            } catch (InvalidVerdictException exception) {
                log.warn("Attempt {}/{} produced an invalid verdict: {}",
                        attempt, properties.maxVerdictAttempts(), exception.getMessage());
                lastFailure = exception;
                prompt = promptBuilder.retryPrompt(evidence, response, exception.getMessage());
            }
        }

        throw new InvalidVerdictException(
                "LLM failed to produce a valid verdict after %d attempts".formatted(properties.maxVerdictAttempts()),
                lastFailure);
    }
}
