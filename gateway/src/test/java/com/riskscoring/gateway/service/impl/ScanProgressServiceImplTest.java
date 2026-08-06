package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.dto.ScanProgressMessage;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.gateway.mapper.ScanMapper;
import com.riskscoring.gateway.repository.ScanRepository;
import com.riskscoring.gateway.websocket.ScanNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScanProgressServiceImplTest {

    @Mock
    private ScanRepository scanRepository;
    @Mock
    private ScanNotifier scanNotifier;
    @Mock
    private MessageSource messageSource;

    private ScanProgressServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ScanProgressServiceImpl(scanRepository, new ScanMapper(), scanNotifier, messageSource);
    }

    @Test
    void applyProgressDoesNothingWhenScanIsUnknown() {
        UUID scanId = UUID.randomUUID();
        when(scanRepository.findById(scanId)).thenReturn(Optional.empty());
        ScanProgress event = progress(scanId, ScanStage.ANALYZING);

        service.applyProgress(event);

        verifyNoInteractions(scanNotifier);
    }

    @Test
    void applyProgressSetsCompletedAtForTerminalStage() {
        Scan scan = scan(ScanStage.ANALYZING);
        when(scanRepository.findById(scan.getId())).thenReturn(Optional.of(scan));
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Completed");
        ScanProgress event = progress(scan.getId(), ScanStage.COMPLETED);

        service.applyProgress(event);

        assertThat(scan.getStatus()).isEqualTo(ScanStage.COMPLETED);
        assertThat(scan.getCompletedAt()).isEqualTo(event.at());
    }

    @Test
    void applyProgressDoesNotSetCompletedAtForNonTerminalStage() {
        Scan scan = scan(ScanStage.PENDING);
        when(scanRepository.findById(scan.getId())).thenReturn(Optional.of(scan));
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Analyzing");
        ScanProgress event = progress(scan.getId(), ScanStage.ANALYZING);

        service.applyProgress(event);

        assertThat(scan.getStatus()).isEqualTo(ScanStage.ANALYZING);
        assertThat(scan.getCompletedAt()).isNull();
    }

    @Test
    void applyProgressNotifiesBothScanAndGroupTopics() {
        Scan scan = scan(ScanStage.PENDING);
        when(scanRepository.findById(scan.getId())).thenReturn(Optional.of(scan));
        when(messageSource.getMessage(any(), any(), any())).thenReturn("Analyzing");
        ScanProgress event = progress(scan.getId(), ScanStage.ANALYZING);

        service.applyProgress(event);

        ScanProgressMessage expected = new ScanMapper().toProgressMessage(event, "Analyzing");
        verify(scanNotifier).notifyProgress(expected);
        verify(scanNotifier).notifyGroupProgress(scan.getGroupId(), expected);
    }

    private static ScanProgress progress(UUID scanId, ScanStage stage) {
        return new ScanProgress(scanId, stage, "console.message.x", List.of(), Language.EN, Instant.now());
    }

    private static Scan scan(ScanStage status) {
        return Scan.builder()
                .id(UUID.randomUUID())
                .groupId(UUID.randomUUID())
                .targetType(ScanTarget.ADDRESS)
                .target("0xabc")
                .chain(Chain.ETHEREUM)
                .status(status)
                .source(com.riskscoring.common.event.ScanSource.USER)
                .requestedAt(Instant.now())
                .build();
    }
}
