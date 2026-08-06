package com.riskscoring.gateway.controller;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.controller.support.AbstractControllerTest;
import com.riskscoring.gateway.controller.support.GatewayControllerTest;
import com.riskscoring.gateway.dto.IssuedSession;
import com.riskscoring.gateway.dto.RegistrationResponse;
import com.riskscoring.gateway.dto.UserView;
import com.riskscoring.gateway.exception.InvalidCredentialsException;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.model.UserStatus;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@GatewayControllerTest(AuthController.class)
class AuthControllerTest extends AbstractControllerTest {

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private GatewayProperties gatewayProperties;

    private final UUID userId = UUID.randomUUID();
    private final UserView userView = new UserView(
            userId, "alice", "Alice", "Doe", "alice@example.com", UserRole.USER, UserStatus.ACTIVE, Language.EN);

    @BeforeEach
    void stubAuthProperties() {
        given(gatewayProperties.auth()).willReturn(new GatewayProperties.Auth(
                "test-jwt-secret-test-jwt-secret", Duration.ofMinutes(15), Duration.ofDays(30),
                5, Duration.ofMinutes(15), false));
    }

    @Test
    void registerReturnsAcceptedWithPendingStatus() throws Exception {
        given(authService.register(any())).willReturn(
                new RegistrationResponse("alice@example.com", UserStatus.PENDING_VERIFICATION));

        mockMvc.perform(registerRequest())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"username": "alice", "firstName": "Alice", "lastName": "Doe",
                                 "email": "alice@example.com", "password": "short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    void verifyEmailReturnsSessionWithRefreshCookie() throws Exception {
        IssuedSession session = new IssuedSession("access-token", "refresh-token", userView);
        given(authService.verifyEmail(any(), nullable(String.class), anyString())).willReturn(session);

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType("application/json")
                        .content("""
                                {"email": "alice@example.com", "code": "123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(cookie().value("refreshToken", "refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void resendCodeReturnsAccepted() throws Exception {
        mockMvc.perform(post("/api/auth/resend-code")
                        .contentType("application/json")
                        .content("""
                                {"email": "alice@example.com"}
                                """))
                .andExpect(status().isAccepted());
    }

    @Test
    void forgotPasswordReturnsAccepted() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType("application/json")
                        .content("""
                                {"email": "alice@example.com"}
                                """))
                .andExpect(status().isAccepted());
    }

    @Test
    void resetPasswordReturnsSessionWithRefreshCookie() throws Exception {
        IssuedSession session = new IssuedSession("access-token", "refresh-token", userView);
        given(authService.resetPassword(any(), nullable(String.class), anyString())).willReturn(session);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content("""
                                {"email": "alice@example.com", "code": "123456", "newPassword": "correct-horse-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().value("refreshToken", "refresh-token"));
    }

    @Test
    void loginReturnsSessionWithRefreshCookie() throws Exception {
        IssuedSession session = new IssuedSession("access-token", "refresh-token", userView);
        given(authService.login(any(), nullable(String.class), anyString())).willReturn(session);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"login": "alice", "password": "correct-horse-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("alice"))
                .andExpect(cookie().value("refreshToken", "refresh-token"));
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        given(authService.login(any(), nullable(String.class), anyString())).willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"login": "alice", "password": "wrong-password-1"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refreshReturnsNewSession() throws Exception {
        IssuedSession session = new IssuedSession("new-access-token", "new-refresh-token", userView);
        given(authService.refresh(eq("old-refresh-token"), nullable(String.class), anyString())).willReturn(session);

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(cookie().value("refreshToken", "new-refresh-token"));
    }

    @Test
    void logoutClearsRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refreshToken", "old-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        AuthenticatedUser principal = new AuthenticatedUser(userId, "alice", UserRole.USER);
        given(authService.currentUser(userId)).willReturn(userView);

        mockMvc.perform(get("/api/auth/me").with(authenticatedAs(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    private static MockHttpServletRequestBuilder registerRequest() {
        return post("/api/auth/register")
                .contentType("application/json")
                .content("""
                        {"username": "alice", "firstName": "Alice", "lastName": "Doe",
                         "email": "alice@example.com", "password": "correct-horse-1"}
                        """);
    }
}
