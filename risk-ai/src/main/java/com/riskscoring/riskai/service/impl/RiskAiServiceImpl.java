package com.riskscoring.riskai.service.impl;

import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Verdict;
import com.riskscoring.riskai.client.LlmClient;
import com.riskscoring.riskai.config.RiskAiProperties;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAiServiceImpl implements RiskAiService {

    private static final String PROGRESS_MESSAGE = "AI is reasoning over the evidence";

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final VerdictParser verdictParser;
    private final ScanReportRepository scanReportRepository;
    private final ScanReportMapper scanReportMapper;
    private final RiskAiEventPublisher eventPublisher;
    private final RiskAiProperties properties;

    @Override
    @Transactional
    public void analyze(SignalsComputed event) {
        if (scanReportRepository.existsByScanId(event.scanId())) {
            log.info("Report for scanId={} already exists, skipping", event.scanId());
            return;
        }

        eventPublisher.publishScanProgress(
                new ScanProgress(event.scanId(), ScanStage.ANALYZING, PROGRESS_MESSAGE, Instant.now()));

        Verdict verdict = askForVerdict(event.evidence());

        scanReportRepository.save(scanReportMapper.toEntity(
                event, verdict, llmClient.model(), properties.promptVersion(), Instant.now()));

        log.info("Verdict for scanId={} is {} score={}", event.scanId(), verdict.riskLevel(), verdict.score());

        eventPublisher.publishScanCompleted(new ScanCompleted(
                event.scanId(),
                event.address(),
                event.chainId(),
                verdict,
                llmClient.model(),
                Instant.now()
        ));
        eventPublisher.publishScanProgress(
                new ScanProgress(event.scanId(), ScanStage.COMPLETED, verdict.riskLevel().name(), Instant.now()));
    }

    private Verdict askForVerdict(EvidenceBundle evidence) {
        String prompt = promptBuilder.userPrompt(evidence);
        InvalidVerdictException lastFailure = null;

        for (int attempt = 1; attempt <= properties.maxVerdictAttempts(); attempt++) {
            String response = llmClient.complete(promptBuilder.systemPrompt(), prompt);
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
