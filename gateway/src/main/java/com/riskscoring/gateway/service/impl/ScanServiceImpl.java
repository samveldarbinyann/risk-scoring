package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.event.ScanRequested;
import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.RecentScanGroupView;
import com.riskscoring.gateway.dto.ScanCreateRequest;
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.dto.ScanGroupChainStatus;
import com.riskscoring.gateway.dto.ScanGroupReportView;
import com.riskscoring.gateway.dto.ScanGroupView;
import com.riskscoring.gateway.dto.ScanHistoryPageView;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.gateway.entity.ScanGroup;
import com.riskscoring.gateway.exception.ChainNotSupportedYetException;
import com.riskscoring.gateway.exception.PublicScanChainLimitException;
import com.riskscoring.gateway.exception.ScanGroupNotFoundException;
import com.riskscoring.gateway.exception.ScanGroupReportNotReadyException;
import com.riskscoring.gateway.exception.ScanNotFoundException;
import com.riskscoring.gateway.exception.ScanReportNotReadyException;
import com.riskscoring.gateway.exception.SingleChainRequiredException;
import com.riskscoring.gateway.exception.TargetChainMismatchException;
import com.riskscoring.gateway.kafka.ScanEventPublisher;
import com.riskscoring.gateway.mapper.ScanMapper;
import com.riskscoring.gateway.model.ScanOwnership;
import com.riskscoring.gateway.model.ScanTargets;
import com.riskscoring.gateway.model.TargetMatch;
import com.riskscoring.gateway.repository.ScanGroupRepository;
import com.riskscoring.gateway.repository.ScanReportRepository;
import com.riskscoring.gateway.repository.ScanRepository;
import com.riskscoring.gateway.repository.ScanRiskSummary;
import com.riskscoring.gateway.service.BillingService;
import com.riskscoring.gateway.service.ChainService;
import com.riskscoring.gateway.service.RateLimitService;
import com.riskscoring.gateway.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScanServiceImpl implements ScanService {

    private final ScanGroupRepository scanGroupRepository;
    private final ScanRepository scanRepository;
    private final ScanReportRepository scanReportRepository;
    private final ScanMapper scanMapper;
    private final ScanEventPublisher scanEventPublisher;
    private final BillingService billingService;
    private final RateLimitService rateLimitService;
    private final ChainService chainService;
    private final GatewayProperties gatewayProperties;

    @Override
    @Transactional
    public ScanGroupAcceptedResponse requestScan(String clientIp, UUID userId, ScanCreateRequest request) {
        rateLimitService.checkPublicScan(clientIp);
        return createScanGroup(requestedChains(request, userId), ScanSource.USER, userId);
    }

    @Override
    @Transactional
    public ScanGroupAcceptedResponse requestApiScan(UUID userId, ScanCreateRequest request) {
        List<TargetMatch> matches = requestedChains(request, userId);
        billingService.chargeQuota(userId, matches.size());
        return createScanGroup(matches, ScanSource.API, userId);
    }

    private ScanGroupAcceptedResponse createScanGroup(List<TargetMatch> matches, ScanSource source, UUID userId) {
        TargetMatch first = matches.getFirst();
        Instant requestedAt = Instant.now();

        ScanGroup group = ScanGroup.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .targetType(first.targetType())
                .target(first.normalizedTarget())
                .requestedAt(requestedAt)
                .build();
        scanGroupRepository.save(group);

        List<Scan> scans = matches.stream()
                .map(match -> Scan.builder()
                        .id(UUID.randomUUID())
                        .groupId(group.getId())
                        .targetType(match.targetType())
                        .target(match.normalizedTarget())
                        .chain(match.chain())
                        .status(ScanStage.PENDING)
                        .source(source)
                        .requestedAt(requestedAt)
                        .build())
                .toList();
        scanRepository.saveAll(scans);

        Language language = Language.fromLocale(LocaleContextHolder.getLocale());
        scans.forEach(scan -> scanEventPublisher.publishScanRequested(scanMapper.toEvent(scan, language, userId)));

        return scanMapper.toGroupAcceptedResponse(group, scans);
    }

    private List<TargetMatch> requestedChains(ScanCreateRequest request, UUID userId) {
        List<TargetMatch> candidates = ScanTargets.classify(request.target());
        List<TargetMatch> explicit = explicitChains(request, candidates);

        if (!explicit.isEmpty()) {
            return singleTargetType(requireWithinPublicLimit(explicit, userId));
        }

        List<TargetMatch> fanOut = candidates.stream()
                .filter(match -> match.chain().scannable())
                .toList();

        if (fanOut.isEmpty()) {
            throw new ChainNotSupportedYetException(candidates.getFirst().chain());
        }

        return singleTargetType(trimToPublicLimit(fanOut, userId));
    }

    private List<TargetMatch> requireWithinPublicLimit(List<TargetMatch> matches, UUID userId) {
        int allowed = gatewayProperties.publicScan().maxChains();

        if (userId == null && matches.size() > allowed) {
            throw new PublicScanChainLimitException(matches.size(), allowed);
        }

        return matches;
    }

    private List<TargetMatch> trimToPublicLimit(List<TargetMatch> matches, UUID userId) {
        int allowed = gatewayProperties.publicScan().maxChains();

        return userId == null ? matches.subList(0, Math.min(matches.size(), allowed)) : matches;
    }

    private List<TargetMatch> explicitChains(ScanCreateRequest request, List<TargetMatch> candidates) {
        if (request.chains() == null) {
            return List.of();
        }

        return request.chains().stream()
                .map(chainService::requireScannable)
                .distinct()
                .map(chain -> candidates.stream()
                        .filter(match -> match.chain() == chain)
                        .findFirst()
                        .orElseThrow(() -> new TargetChainMismatchException(request.target(), chain)))
                .toList();
    }

    private List<TargetMatch> singleTargetType(List<TargetMatch> matches) {
        boolean anyTransaction = matches.stream().anyMatch(match -> match.targetType() == ScanTarget.TRANSACTION);

        if (matches.size() > 1 && anyTransaction) {
            throw new SingleChainRequiredException(matches.size());
        }

        return matches;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentScanGroupView> getRecentScans(UUID userId) {
        List<ScanGroup> groups = scanGroupRepository.findTop5ByUserIdOrderByRequestedAtDesc(userId);
        return toViews(groups);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanHistoryPageView getScanHistory(UUID userId, ScanSource source, int page, int size) {
        int clampedSize = Math.clamp(size, 1, 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampedSize);
        Page<ScanGroup> result = scanGroupRepository.findHistory(userId, source, pageable);

        return new ScanHistoryPageView(
                toViews(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Override
    @Transactional
    public void ingestMonitorScan(ScanRequested event) {
        if (scanRepository.existsById(event.scanId())) {
            return;
        }

        ScanGroup group = ScanGroup.builder()
                .id(UUID.randomUUID())
                .userId(event.userId())
                .targetType(event.targetType())
                .target(event.target())
                .requestedAt(event.requestedAt())
                .build();
        scanGroupRepository.save(group);

        Scan scan = Scan.builder()
                .id(event.scanId())
                .groupId(group.getId())
                .targetType(event.targetType())
                .target(event.target())
                .chain(event.chain())
                .status(ScanStage.PENDING)
                .source(event.source())
                .requestedAt(event.requestedAt())
                .build();
        scanRepository.save(scan);
    }

    private List<RecentScanGroupView> toViews(List<ScanGroup> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }

        List<UUID> groupIds = groups.stream().map(ScanGroup::getId).toList();
        List<Scan> scans = scanRepository.findByGroupIdIn(groupIds);
        Map<UUID, List<Scan>> scansByGroupId = scans.stream().collect(Collectors.groupingBy(Scan::getGroupId));

        List<UUID> completedScanIds = scans.stream()
                .filter(scan -> scan.getStatus() == ScanStage.COMPLETED)
                .map(Scan::getId)
                .toList();
        Map<UUID, ScanRiskSummary> riskByScanId = scanReportRepository.findRiskSummaries(completedScanIds).stream()
                .collect(Collectors.toMap(ScanRiskSummary::scanId, Function.identity()));

        return groups.stream()
                .map(group -> scanMapper.toRecentScanView(
                        group,
                        scansByGroupId.getOrDefault(group.getId(), List.of()),
                        riskByScanId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScanGroupView getScanGroup(UUID groupId, UUID requesterId) {
        requireGroupAccess(groupId, requesterId);
        return groupView(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanGroupReportView getScanGroupReport(UUID groupId, UUID requesterId) {
        requireGroupAccess(groupId, requesterId);

        ScanGroupView group = groupView(groupId);
        if (!group.completed()) {
            throw new ScanGroupReportNotReadyException(groupId);
        }

        List<ScanReportView> reports = group.chains().stream()
                .filter(chain -> chain.status() == ScanStage.COMPLETED)
                .sorted(Comparator.comparing(ScanGroupChainStatus::chain))
                .map(chain -> scanReportRepository.findByScanId(chain.scanId()))
                .flatMap(Optional::stream)
                .map(scanMapper::toReportView)
                .toList();

        return new ScanGroupReportView(groupId, group.targetType(), group.target(), reports);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanView getScan(UUID scanId, UUID requesterId) {
        return scanMapper.toView(requireScanAccess(scanId, requesterId));
    }

    @Override
    @Transactional(readOnly = true)
    public ScanReportView getScanReport(UUID scanId, UUID requesterId) {
        Scan scan = requireScanAccess(scanId, requesterId);

        if (scan.getStatus() != ScanStage.COMPLETED) {
            throw new ScanReportNotReadyException(scanId, scan.getStatus());
        }

        return scanReportRepository.findByScanId(scanId)
                .map(scanMapper::toReportView)
                .orElseThrow(() -> new ScanReportNotReadyException(scanId, scan.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessGroup(UUID groupId, UUID requesterId) {
        return accessibleGroup(groupId, requesterId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessScan(UUID scanId, UUID requesterId) {
        return scanRepository.findById(scanId)
                .flatMap(scan -> accessibleGroup(scan.getGroupId(), requesterId))
                .isPresent();
    }

    private ScanGroupView groupView(UUID groupId) {
        List<Scan> scans = scanRepository.findByGroupId(groupId);
        if (scans.isEmpty()) {
            throw new ScanGroupNotFoundException(groupId);
        }

        return scanMapper.toGroupView(groupId, scans);
    }

    private Optional<ScanGroup> accessibleGroup(UUID groupId, UUID requesterId) {
        return scanGroupRepository.findById(groupId)
                .filter(group -> ScanOwnership.isAccessible(group.getUserId(), requesterId));
    }

    private void requireGroupAccess(UUID groupId, UUID requesterId) {
        accessibleGroup(groupId, requesterId)
                .orElseThrow(() -> new ScanGroupNotFoundException(groupId));
    }

    private Scan requireScanAccess(UUID scanId, UUID requesterId) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new ScanNotFoundException(scanId));

        return accessibleGroup(scan.getGroupId(), requesterId)
                .map(group -> scan)
                .orElseThrow(() -> new ScanNotFoundException(scanId));
    }
}