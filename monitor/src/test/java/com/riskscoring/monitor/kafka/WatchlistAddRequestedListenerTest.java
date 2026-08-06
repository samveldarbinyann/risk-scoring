package com.riskscoring.monitor.kafka;

import com.riskscoring.common.event.WatchlistAddRequested;
import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.Language;
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
class WatchlistAddRequestedListenerTest {

    @Mock
    private WatchlistService watchlistService;

    private WatchlistAddRequestedListener listener;

    @BeforeEach
    void setUp() {
        listener = new WatchlistAddRequestedListener(watchlistService);
    }

    @Test
    void onWatchlistAddRequestedDelegatesToWatchlistService() {
        WatchlistAddRequested event = new WatchlistAddRequested(
                UUID.randomUUID(), UUID.randomUUID(), "0xtarget", Chain.ETHEREUM, Language.EN, Instant.now());

        listener.onWatchlistAddRequested(event);

        verify(watchlistService).addEntry(event);
    }
}
