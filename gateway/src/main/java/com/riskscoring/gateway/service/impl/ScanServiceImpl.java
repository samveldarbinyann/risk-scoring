package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.event.ScanSource;
import com.riskscoring.common.event.ScanStage;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.dto.RecentScanGroupView;
import com.riskscoring.gateway.dto.ScanCreateRequest;
import com.riskscoring.gateway.dto.ScanGroupAcceptedResponse;
import com.riskscoring.gateway.dto.ScanGroupChainStatus;
import com.riskscoring.gateway.dto.ScanGroupReportView;
import com.riskscoring.gateway.dto.ScanGroupView;
import com.riskscoring.gateway.dto.ScanReportView;
import com.riskscoring.gateway.dto.ScanView;
import com.riskscoring.gateway.entity.Scan;
import com.riskscoring.gateway.entity.ScanGroup;
import com.riskscoring.gateway.exception.ChainNotSupportedYetException;
import com.riskscoring.gateway.exception.ScanGroupNotFoundException;
import com.riskscoring.gateway.exception.ScanGroupReportNotReadyException;
import com.riskscoring.gateway.exception.ScanNotFoundException;
import com.riskscoring.gateway.exception.ScanReportNotReadyException;
import com.riskscoring.gateway.exception.SingleChainRequiredException;
import com.riskscoring.gateway.exception.TargetChainMismatchException;
import com.riskscoring.gateway.kafka.ScanEventPublisher;
import com.riskscoring.gateway.mapper.ScanMapper;
import com.riskscoring.gateway.model.ScanTargets;
import com.riskscoring.gateway.model.TargetMatch;
import com.riskscoring.gateway.repository.ScanGroupRepository;
import com.riskscoring.gateway.repository.ScanReportRepository;
import com.riskscoring.gateway.repository.ScanRepository;
import com.riskscoring.gateway.repository.ScanRiskSummary;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.BillingService;
import com.riskscoring.gateway.service.ChainService;
import com.riskscoring.gateway.service.RateLimitService;
import com.riskscoring.gateway.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Override
    @Transactional
    public ScanGroupAcceptedResponse requestScan(String clientIp, ScanCreateRequest request) {
        rateLimitService.checkPublicScan(clientIp);
        return createScanGroup(requestedChains(request), ScanSource.USER, currentUserId());
    }

    @Override
    @Transactional
    public ScanGroupAcceptedResponse requestApiScan(UUID userId, ScanCreateRequest request) {
        List<TargetMatch> matches = requestedChains(request);
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
        scans.forEach(scan -> scanEventPublisher.publishScanRequested(scanMapper.toEvent(scan, language)));

        return scanMapper.toGroupAcceptedResponse(group, scans);
    }

    private List<TargetMatch> requestedChains(ScanCreateRequest request) {
        List<TargetMatch> candidates = ScanTargets.classify(request.target());
        List<TargetMatch> explicit = explicitChains(request, candidates);

        if (!explicit.isEmpty()) {
            return singleTargetType(explicit);
        }

        List<TargetMatch> fanOut = candidates.stream()
                .filter(match -> match.chain().scannable())
                .toList();

        if (fanOut.isEmpty()) {
            throw new ChainNotSupportedYetException(candidates.getFirst().chain());
        }

        return singleTargetType(fanOut);
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

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user.id();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentScanGroupView> getRecentScans(UUID userId) {
        List<ScanGroup> groups = scanGroupRepository.findTop5ByUserIdOrderByRequestedAtDesc(userId);
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
    public ScanGroupView getScanGroup(UUID groupId) {
        List<Scan> scans = scanRepository.findByGroupId(groupId);
        if (scans.isEmpty()) {
            throw new ScanGroupNotFoundException(groupId);
        }
        return scanMapper.toGroupView(groupId, scans);
    }

    @Override
    @Transactional(readOnly = true)
    public ScanGroupReportView getScanGroupReport(UUID groupId) {
        ScanGroupView group = getScanGroup(groupId);
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
    public ScanView getScan(UUID scanId) {
        return scanRepository.findById(scanId)
                .map(scanMapper::toView)
                .orElseThrow(() -> new ScanNotFoundException(scanId));
    }

    @Override
    @Transactional(readOnly = true)
    public ScanReportView getScanReport(UUID scanId) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new ScanNotFoundException(scanId));

        if (scan.getStatus() != ScanStage.COMPLETED) {
            throw new ScanReportNotReadyException(scanId, scan.getStatus());
        }

        return scanReportRepository.findByScanId(scanId)
                .map(scanMapper::toReportView)
                .orElseThrow(() -> new ScanReportNotReadyException(scanId, scan.getStatus()));
    }
}