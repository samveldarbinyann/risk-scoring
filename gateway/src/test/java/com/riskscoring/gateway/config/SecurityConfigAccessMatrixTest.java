package com.riskscoring.gateway.config;

import com.riskscoring.gateway.controller.ScanController;
import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.security.ApiKeyAuthenticationFilter;
import com.riskscoring.gateway.security.JwtAuthenticationFilter;
import com.riskscoring.gateway.security.RestSecurityErrorHandler;
import com.riskscoring.gateway.service.ApiKeyService;
import com.riskscoring.gateway.service.ScanService;
import com.riskscoring.gateway.service.TokenService;
import com.riskscoring.gateway.support.GatewayPropertiesFixture;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ScanController.class)
@Import({SecurityConfig.class, LocaleConfig.class, JwtAuthenticationFilter.class,
        ApiKeyAuthenticationFilter.class, RestSecurityErrorHandler.class,
        SecurityConfigAccessMatrixTest.Beans.class})
class SecurityConfigAccessMatrixTest extends AbstractControllerTest {

    private static final UUID ID = UUID.fromString("2c2f0a0a-6b4c-4a71-9a55-0c3b2a1d4e90");

    @MockitoBean
    private ScanService scanService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @Test
    void anonymousMayStartAScan() throws Exception {
        mockMvc.perform(post("/api/scans")
                        .contentType("application/json")
                        .content("""
                                {"target": "0xabc"}
                                """))
                .andExpect(passesSecurityChain());
    }

    @Test
    void anonymousMayReadScanAndGroupEndpoints() throws Exception {
        for (String path : List.of(
                "/api/scans/" + ID,
                "/api/scans/" + ID + "/report",
                "/api/scans/groups/" + ID,
                "/api/scans/groups/" + ID + "/report")) {
            mockMvc.perform(get(path)).andExpect(passesSecurityChain());
        }
    }

    @Test
    void anonymousIsRejectedOnScanHistory() throws Exception {
        mockMvc.perform(get("/api/scans")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousIsRejectedOnRecentScans() throws Exception {
        mockMvc.perform(get("/api/scans/recent")).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousIsRejectedOnTheApiKeySurface() throws Exception {
        mockMvc.perform(post("/api/v1/scans")
                        .contentType("application/json")
                        .content("""
                                {"target": "0xabc"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unmappedPathsAreDeniedByDefault() throws Exception {
        mockMvc.perform(get("/api/anything-else")).andExpect(status().isUnauthorized());
    }

    private static ResultMatcher passesSecurityChain() {
        return result -> {
            int actual = result.getResponse().getStatus();
            if (actual == 401 || actual == 403) {
                throw new AssertionError("Request was blocked by the security chain with " + actual);
            }
        };
    }

    @TestConfiguration
    static class Beans {

        @Bean
        MessageSource messageSource() {
            StaticMessageSource messageSource = new StaticMessageSource();
            messageSource.setUseCodeAsDefaultMessage(true);
            return messageSource;
        }

        @Bean
        GatewayProperties gatewayProperties() {
            return GatewayPropertiesFixture.gatewayProperties();
        }
    }
}
