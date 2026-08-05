package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.event.ScanProgress;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.gateway.dto.ScanProgressMessage;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.gateway.mapper.ScanMapper;
import com.riskscoring.gateway.repository.ScanRepository;
import com.riskscoring.gateway.service.ScanProgressService;
import com.riskscoring.gateway.websocket.ScanNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanProgressServiceImpl implements ScanProgressService {

    private static final Set<ScanStage> TERMINAL_STAGES = Set.of(ScanStage.COMPLETED, ScanStage.FAILED);

    private final ScanRepository scanRepository;
    private final ScanMapper scanMapper;
    private final ScanNotifier scanNotifier;
    private final MessageSource messageSource;

    @Override
    @Transactional
    public void applyProgress(ScanProgress event) {
        scanRepository.findById(event.scanId()).ifPresentOrElse(
                scan -> advance(scan, event),
                () -> log.warn("Progress for unknown scanId={}, ignored", event.scanId())
        );
    }

    private void advance(Scan scan, ScanProgress event) {
        scan.setStatus(event.stage());

        if (TERMINAL_STAGES.contains(event.stage())) {
            scan.setCompletedAt(event.at());
        }

        String resolvedMessage = messageSource.getMessage(
                event.messageKey(), event.messageArgs().toArray(), event.language().toLocale());
        ScanProgressMessage message = scanMapper.toProgressMessage(event, resolvedMessage);
        scanNotifier.notifyProgress(message);
        scanNotifier.notifyGroupProgress(scan.getGroupId(), message);
        log.info("Scan {} (group {}) moved to {}", scan.getId(), scan.getGroupId(), event.stage());
    }
}