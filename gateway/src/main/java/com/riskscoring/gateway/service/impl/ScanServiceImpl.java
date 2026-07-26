package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.model.EvmChain;
import com.riskscoring.gateway.dto.ScanAcceptedResponse;
import com.riskscoring.gateway.dto.ScanCreateRequest;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.gateway.exception.ScanNotFoundException;
import com.riskscoring.gateway.exception.UnsupportedChainException;
import com.riskscoring.gateway.kafka.ScanEventPublisher;
import com.riskscoring.gateway.mapper.ScanMapper;
import com.riskscoring.gateway.repository.ScanRepository;
import com.riskscoring.gateway.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScanServiceImpl implements ScanService {

    private final ScanRepository scanRepository;
    private final ScanMapper scanMapper;
    private final ScanEventPublisher scanEventPublisher;

    @Override
    @Transactional
    public ScanAcceptedResponse requestScan(ScanCreateRequest request) {
        EvmChain.byId(request.chainId())
                .orElseThrow(() -> new UnsupportedChainException(request.chainId()));

        Scan scan = Scan.builder()
                .id(UUID.randomUUID())
                .address(request.address().toLowerCase(Locale.ROOT))
                .chainId(request.chainId())
                .status(ScanStage.PENDING)
                .source(ScanSource.USER)
                .requestedAt(Instant.now())
                .build();

        scanRepository.save(scan);
        scanEventPublisher.publishScanRequested(scanMapper.toEvent(scan));

        return scanMapper.toAcceptedResponse(scan);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanView getScan(UUID scanId) {
        return scanRepository.findById(scanId)
                .map(scanMapper::toView)
                .orElseThrow(() -> new ScanNotFoundException(scanId));
    }
}