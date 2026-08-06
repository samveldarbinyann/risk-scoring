package com.riskscoring.riskai.service.impl;

import com.riskscoring.common.event.ScanCompleted;
import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.event.SignalsComputed;
import com.riskscoring.common.model.AddressEvidence;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.EvidenceBundle;
import com.riskscoring.common.model.Heuristics;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.common.model.Verdict;
import com.riskscoring.riskai.client.LlmClient;
import com.riskscoring.riskai.config.RiskAiProperties;
import com.riskscoring.riskai.entity.ScanReport;
import com.riskscoring.riskai.exception.InvalidVerdictException;
import com.riskscoring.riskai.kafka.RiskAiEventPublisher;
import com.riskscoring.riskai.mapper.ScanReportMapper;
import com.riskscoring.riskai.repository.ScanReportRepository;
import com.riskscoring.riskai.service.PromptBuilder;
import com.riskscoring.riskai.service.VerdictParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAiServiceImplTest {

    private static final UUID SCAN_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2024-06-01T00:00:00Z");

    @Mock
    private LlmClient llmClient;
    @Mock
    private PromptBuilder promptBuilder;
    @Mock
    private VerdictParser verdictParser;
    @Mock
    private ScanReportRepository scanReportRepository;
    @Mock
    private ScanReportMapper scanReportMapper;
    @Mock
    private RiskAiEventPublisher eventPublisher;

    private RiskAiServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(llmClient.model()).thenReturn("deepseek-chat");
        service = new RiskAiServiceImpl(llmClient, promptBuilder, verdictParser, scanReportRepository,
                scanReportMapper, eventPublisher, properties(2));
    }

    @Test
    void analyzeRepublishesExistingReportWithoutCallingLlmWhenAlreadyScanned() {
        ScanReport stored = storedReport();
        Verdict verdict = verdict(RiskLevel.LOW);
        when(scanReportRepository.findByScanId(SCAN_ID)).thenReturn(Optional.of(stored));
        when(scanReportMapper.toVerdict(stored)).thenReturn(verdict);

        service.analyze(event());

        verifyNoInteractions(promptBuilder, verdictParser);
        verify(llmClient, never()).complete(any(), any());
        verify(scanReportRepository, never()).save(any());
        verify(eventPublisher, times(1)).publishScanProgress(any());

        ArgumentCaptor<ScanCompleted> captor = ArgumentCaptor.forClass(ScanCompleted.class);
        verify(eventPublisher).publishScanCompleted(captor.capture());
        assertThat(captor.getValue().model()).isEqualTo(stored.getModel());
        assertThat(captor.getValue().completedAt()).isEqualTo(stored.getCreatedAt());
        assertThat(captor.getValue().verdict()).isEqualTo(verdict);
    }

    @Test
    void analyzePublishesAnalyzingProgressBeforeCallingLlmWhenNoExistingReport() {
        when(scanReportRepository.findByScanId(SCAN_ID)).thenReturn(Optional.empty());
        stubValidVerdictOnFirstAttempt();

        service.analyze(event());

        InOrder order = inOrder(eventPublisher, llmClient);
        order.verify(eventPublisher).publishScanProgress(argThatStage(ScanStage.ANALYZING));
        order.verify(llmClient).complete(any(), any());
    }

    @Test
    void analyzeCallsLlmOnceAndSavesReportWhenFirstAttemptValid() {
        when(scanReportRepository.findByScanId(SCAN_ID)).thenReturn(Optional.empty());
        stubValidVerdictOnFirstAttempt();
        ScanReport mappedReport = storedReport();
        when(scanReportMapper.toEntity(any(), any(), eq("deepseek-chat"), eq("v1"), any())).thenReturn(mappedReport);

        service.analyze(event());

        verify(llmClient, times(1)).complete(any(), any());
        verify(scanReportRepository).save(mappedReport);

        ArgumentCaptor<ScanProgress> progressCaptor = ArgumentCaptor.forClass(ScanProgress.class);
        verify(eventPublisher, times(2)).publishScanProgress(progressCaptor.capture());
        assertThat(progressCaptor.getAllValues().get(1).stage()).isEqualTo(ScanStage.COMPLETED);
        assertThat(progressCaptor.getAllValues().get(1).messageKey()).isEqualTo("risk.level.LOW");

        ArgumentCaptor<ScanCompleted> completedCaptor = ArgumentCaptor.forClass(ScanCompleted.class);
        verify(eventPublisher).publishScanCompleted(completedCaptor.capture());
        assertThat(completedCaptor.getValue().model()).isEqualTo("deepseek-chat");
    }

    @Test
    void analyzeRetriesWithRetryPromptWhenFirstAttemptInvalid() {
        when(scanReportRepository.findByScanId(SCAN_ID)).thenReturn(Optional.empty());
        when(promptBuilder.systemPrompt(any(), any())).thenReturn("system prompt");
        when(promptBuilder.userPrompt(any())).thenReturn("user prompt");
        when(promptBuilder.retryPrompt(any(), any(), any())).thenReturn("retry prompt");
        when(llmClient.complete("system prompt", "user prompt")).thenReturn("bad response");
        when(llmClient.complete("system prompt", "retry prompt")).thenReturn("good response");
        InvalidVerdictException failure = new InvalidVerdictException("riskLevel is missing");
        when(verdictParser.parse("bad response")).thenThrow(failure);
        when(verdictParser.parse("good response")).thenReturn(verdict(RiskLevel.LOW));
        when(scanReportMapper.toEntity(any(), any(), any(), any(), any())).thenReturn(storedReport());

        service.analyze(event());

        verify(llmClient, times(2)).complete(any(), any());
        verify(promptBuilder).retryPrompt(any(), eq("bad response"), eq("riskLevel is missing"));
    }

    @Test
    void analyzeThrowsInvalidVerdictExceptionAfterExhaustingMaxAttempts() {
        when(scanReportRepository.findByScanId(SCAN_ID)).thenReturn(Optional.empty());
        when(promptBuilder.systemPrompt(any(), any())).thenReturn("system prompt");
        when(promptBuilder.userPrompt(any())).thenReturn("user prompt");
        when(promptBuilder.retryPrompt(any(), any(), any())).thenReturn("retry prompt");
        when(llmClient.complete(any(), any())).thenReturn("bad response");
        when(verdictParser.parse(any())).thenThrow(new InvalidVerdictException("riskLevel is missing"));

        assertThatThrownBy(() -> service.analyze(event()))
                .isInstanceOf(InvalidVerdictException.class)
                .hasMessageContaining("after 2 attempts");

        verify(llmClient, times(2)).complete(any(), any());
        verify(scanReportRepository, never()).save(any());
        verify(eventPublisher, never()).publishScanCompleted(any());
    }

    private void stubValidVerdictOnFirstAttempt() {
        when(promptBuilder.systemPrompt(any(), any())).thenReturn("system prompt");
        when(promptBuilder.userPrompt(any())).thenReturn("user prompt");
        when(llmClient.complete(any(), any())).thenReturn("good response");
        when(verdictParser.parse("good response")).thenReturn(verdict(RiskLevel.LOW));
    }

    private static ScanProgress argThatStage(ScanStage stage) {
        return org.mockito.ArgumentMatchers.argThat(progress -> progress != null && progress.stage() == stage);
    }

    private static RiskAiProperties properties(int maxAttempts) {
        return new RiskAiProperties(
                new RiskAiProperties.Llm("http://localhost", "test-key", "deepseek-chat", 0.2,
                        Duration.ofSeconds(5), Duration.ofSeconds(30)),
                maxAttempts, "v1");
    }

    private static SignalsComputed event() {
        EvidenceBundle evidence = new AddressEvidence("0xtarget", Chain.ETHEREUM, NOW, null, 0, 0, false, "0",
                List.of(), 0, List.of(), null, new Heuristics(null, null, false, 0, 0));
        return new SignalsComputed(SCAN_ID, ScanTarget.ADDRESS, "0xtarget", Chain.ETHEREUM, evidence, Language.EN, NOW);
    }

    private static Verdict verdict(RiskLevel riskLevel) {
        return new Verdict(riskLevel, 10, "clean wallet", List.of(), List.of());
    }

    private static ScanReport storedReport() {
        return ScanReport.builder()
                .id(UUID.randomUUID())
                .scanId(SCAN_ID)
                .targetType(ScanTarget.ADDRESS)
                .target("0xtarget")
                .chain(Chain.ETHEREUM)
                .riskLevel(RiskLevel.LOW)
                .score(10)
                .explanation("clean wallet")
                .decisiveSignals("[]")
                .manualChecks("[]")
                .observedAt(NOW)
                .evidence("{}")
                .model("deepseek-chat")
                .promptVersion("v1")
                .createdAt(NOW)
                .build();
    }
}
