package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
import com.riskscoring.common.model.ScanTarget;
import com.riskscoring.gateway.dto.WatchlistCreateRequest;
import com.riskscoring.gateway.dto.WatchlistEntryView;
import com.riskscoring.gateway.exception.WatchlistEntryNotFoundException;
import com.riskscoring.gateway.kafka.WatchlistEventPublisher;
import com.riskscoring.gateway.mapper.WatchlistMapper;
import com.riskscoring.gateway.model.ScanTargets;
import com.riskscoring.gateway.model.TargetMatch;
import com.riskscoring.gateway.repository.WatchlistRepository;
import com.riskscoring.gateway.service.ChainService;
import com.riskscoring.gateway.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistEventPublisher watchlistEventPublisher;
    private final WatchlistMapper watchlistMapper;
    private final ChainService chainService;

    @Override
    public void addToWatchlist(UUID userId, WatchlistCreateRequest request) {
        Chain chain = chainService.requireScannable(request.chain());
        TargetMatch match = ScanTargets.require(request.address(), chain, ScanTarget.ADDRESS);

        Language language = Language.fromLocale(LocaleContextHolder.getLocale());

        watchlistEventPublisher.publishWatchlistAddRequested(
                watchlistMapper.toAddRequested(userId, match.normalizedTarget(), chain, language));
    }

    @Override
    public void removeFromWatchlist(UUID userId, UUID entryId) {
        if (!watchlistRepository.existsByIdAndUserId(entryId, userId)) {
            throw new WatchlistEntryNotFoundException(entryId);
        }

        watchlistEventPublisher.publishWatchlistRemoveRequested(
                watchlistMapper.toRemoveRequested(userId, entryId));
    }

    @Override
    public List<WatchlistEntryView> listWatchlist(UUID userId) {
        return watchlistRepository.findAllByUserId(userId).stream()
                .map(watchlistMapper::toView)
                .toList();
    }
}
