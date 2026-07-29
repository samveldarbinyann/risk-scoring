package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.dto.AuthResponse;
import com.riskscoring.gateway.dto.IssuedSession;
import com.riskscoring.gateway.dto.LoginRequest;
import com.riskscoring.gateway.dto.RegisterRequest;
import com.riskscoring.gateway.dto.RegistrationResponse;
import com.riskscoring.gateway.dto.ResendCodeRequest;
import com.riskscoring.gateway.dto.UserView;
import com.riskscoring.gateway.dto.VerifyEmailRequest;
import com.riskscoring.gateway.security.AuthenticatedUser;
import com.riskscoring.gateway.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";
    private static final String SAME_SITE = "Lax";

    private final AuthService authService;
    private final GatewayProperties gatewayProperties;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request,
                                                    HttpServletRequest httpRequest) {
        return sessionResponse(authService.verifyEmail(request, userAgent(httpRequest), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/resend-code")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendCode(@Valid @RequestBody ResendCodeRequest request) {
        authService.resendVerificationCode(request.email());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        return sessionResponse(authService.login(request, userAgent(httpRequest), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
                                                HttpServletRequest httpRequest) {
        return sessionResponse(authService.refresh(refreshToken, userAgent(httpRequest), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.currentUser(user.id());
    }

    private ResponseEntity<AuthResponse> sessionResponse(IssuedSession session) {
        AuthResponse body = new AuthResponse(
                session.accessToken(),
                session.accessTokenTtl().toSeconds(),
                session.user());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookie(session.refreshToken(), session.refreshTokenTtl()).toString())
                .body(body);
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(gatewayProperties.auth().secureCookie())
                .sameSite(SAME_SITE)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }
}
