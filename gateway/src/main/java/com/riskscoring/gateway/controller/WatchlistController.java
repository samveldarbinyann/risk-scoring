package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.dto.WatchlistCreateRequest;
import com.riskscoring.gateway.dto.WatchlistEntryView;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.WatchlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void addToWatchlist(@AuthenticationPrincipal AuthenticatedUser user,
                                @Valid @RequestBody WatchlistCreateRequest request) {
        watchlistService.addToWatchlist(user.id(), request);
    }

    @GetMapping
    public List<WatchlistEntryView> listWatchlist(@AuthenticationPrincipal AuthenticatedUser user) {
        return watchlistService.listWatchlist(user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void removeFromWatchlist(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        watchlistService.removeFromWatchlist(user.id(), id);
    }
}
