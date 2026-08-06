package com.riskscoring.gateway.controller;

import com.riskscoring.common.model.Chain;
import com.riskscoring.common.model.RiskLevel;
import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.controller.support.GatewayControllerTest;
import com.riskscoring.gateway.dto.WatchlistEntryView;
import com.riskscoring.gateway.exception.WatchlistEntryNotFoundException;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@GatewayControllerTest(WatchlistController.class)
class WatchlistControllerTest extends AbstractControllerTest {

    @MockitoBean
    private WatchlistService watchlistService;

    private final AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "alice", UserRole.USER);

    @Test
    void addToWatchlistDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/watchlist").with(authenticatedAs(user))
                        .contentType("application/json")
                        .content("""
                                {"address": "0xabc", "chain": "ETHEREUM"}
                                """))
                .andExpect(status().isAccepted());

        verify(watchlistService).addToWatchlist(eq(user.id()), any());
    }

    @Test
    void addToWatchlistRejectsBlankAddress() throws Exception {
        mockMvc.perform(post("/api/watchlist").with(authenticatedAs(user))
                        .contentType("application/json")
                        .content("""
                                {"address": "", "chain": "ETHEREUM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void listWatchlistReturnsEntriesForCurrentUser() throws Exception {
        WatchlistEntryView entry = new WatchlistEntryView(
                UUID.randomUUID(), "0xabc", Chain.ETHEREUM, RiskLevel.LOW, 10, null, Instant.now(), Instant.now());
        given(watchlistService.listWatchlist(user.id())).willReturn(List.of(entry));

        mockMvc.perform(get("/api/watchlist").with(authenticatedAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].address").value("0xabc"))
                .andExpect(jsonPath("$[0].chain").value("ETHEREUM"));
    }

    @Test
    void removeFromWatchlistDelegatesToService() throws Exception {
        UUID entryId = UUID.randomUUID();

        mockMvc.perform(delete("/api/watchlist/{id}", entryId).with(authenticatedAs(user)))
                .andExpect(status().isAccepted());

        verify(watchlistService).removeFromWatchlist(user.id(), entryId);
    }

    @Test
    void removeFromWatchlistReturnsNotFoundForUnknownEntry() throws Exception {
        UUID entryId = UUID.randomUUID();
        doThrow(new WatchlistEntryNotFoundException(entryId))
                .when(watchlistService).removeFromWatchlist(user.id(), entryId);

        mockMvc.perform(delete("/api/watchlist/{id}", entryId).with(authenticatedAs(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("WATCHLIST_ENTRY_NOT_FOUND"));
    }
}
