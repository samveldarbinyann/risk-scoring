package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.EvmChain;
import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.dto.WatchlistCreateRequest;
import com.riskscoring.gateway.dto.WatchlistEntryView;
import com.riskscoring.gateway.exception.UnsupportedChainException;
import com.riskscoring.gateway.exception.WatchlistEntryNotFoundException;
import com.riskscoring.gateway.kafka.WatchlistEventPublisher;
import com.riskscoring.gateway.mapper.WatchlistMapper;
import com.riskscoring.gateway.model.EvmAddresses;
import com.riskscoring.gateway.repository.WatchlistRepository;
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

    @Override
    public void addToWatchlist(UUID userId, WatchlistCreateRequest request) {
        String address = EvmAddresses.normalize(request.address());
        EvmChain chain = EvmChain.byId(request.chainId())
                .orElseThrow(() -> new UnsupportedChainException(request.chainId()));
        Language language = Language.fromLocale(LocaleContextHolder.getLocale());

        watchlistEventPublisher.publishWatchlistAddRequested(
                watchlistMapper.toAddRequested(userId, address, chain.chainId(), language));
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