package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.controller.support.GatewayControllerTest;
import com.riskscoring.gateway.dto.ApiKeyCreatedView;
import com.riskscoring.gateway.dto.ApiKeyView;
import com.riskscoring.gateway.exception.ApiKeyNotFoundException;
import com.riskscoring.gateway.model.ApiKeyStatus;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.ApiKeyService;
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

@GatewayControllerTest(ApiKeyController.class)
class ApiKeyControllerTest extends AbstractControllerTest {

    @MockitoBean
    private ApiKeyService apiKeyService;

    private final AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "alice", UserRole.USER);

    @Test
    void createReturnsCreatedApiKey() throws Exception {
        ApiKeyCreatedView created = new ApiKeyCreatedView(
                UUID.randomUUID(), "ci-key", "rsk_abcd", "rsk_abcd1234secret", ApiKeyStatus.ACTIVE, Instant.now());
        given(apiKeyService.create(eq(user.id()), any())).willReturn(created);

        mockMvc.perform(post("/api/api-keys").with(authenticatedAs(user))
                        .contentType("application/json")
                        .content("""
                                {"name": "ci-key"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ci-key"))
                .andExpect(jsonPath("$.apiKey").value("rsk_abcd1234secret"));
    }

    @Test
    void createRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/api-keys").with(authenticatedAs(user))
                        .contentType("application/json")
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void listReturnsKeysForCurrentUser() throws Exception {
        ApiKeyView view = new ApiKeyView(
                UUID.randomUUID(), "ci-key", "rsk_abcd", ApiKeyStatus.ACTIVE, null, Instant.now(), null);
        given(apiKeyService.list(user.id())).willReturn(List.of(view));

        mockMvc.perform(get("/api/api-keys").with(authenticatedAs(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ci-key"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void revokeDelegatesToService() throws Exception {
        UUID keyId = UUID.randomUUID();

        mockMvc.perform(delete("/api/api-keys/{id}", keyId).with(authenticatedAs(user)))
                .andExpect(status().isNoContent());

        verify(apiKeyService).revoke(user.id(), keyId);
    }

    @Test
    void revokeUnknownKeyReturnsNotFound() throws Exception {
        UUID keyId = UUID.randomUUID();
        doThrow(new ApiKeyNotFoundException(keyId)).when(apiKeyService).revoke(user.id(), keyId);

        mockMvc.perform(delete("/api/api-keys/{id}", keyId).with(authenticatedAs(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("API_KEY_NOT_FOUND"));
    }
}
