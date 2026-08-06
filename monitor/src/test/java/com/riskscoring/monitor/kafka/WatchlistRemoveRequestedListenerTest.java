package com.riskscoring.monitor.kafka;

import com.riskscoring.common.event.WatchlistRemoveRequested;
import com.riskscoring.monitor.service.WatchlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchlistRemoveRequestedListenerTest {

    @Mock
    private WatchlistService watchlistService;

    private WatchlistRemoveRequestedListener listener;

    @BeforeEach
    void setUp() {
        listener = new WatchlistRemoveRequestedListener(watchlistService);
    }

    @Test
    void onWatchlistRemoveRequestedDelegatesToWatchlistService() {
        WatchlistRemoveRequested event = new WatchlistRemoveRequested(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        listener.onWatchlistRemoveRequested(event);

        verify(watchlistService).removeEntry(event);
    }
}
