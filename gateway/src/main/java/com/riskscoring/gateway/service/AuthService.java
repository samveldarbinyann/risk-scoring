package com.riskscoring.gateway.service;

import com.riskscoring.gateway.dto.IssuedSession;
import com.riskscoring.gateway.dto.LoginRequest;
import com.riskscoring.gateway.dto.RegisterRequest;
import com.riskscoring.gateway.dto.RegistrationResponse;
import com.riskscoring.gateway.dto.UserView;
import com.riskscoring.gateway.dto.VerifyEmailRequest;

import java.util.UUID;

public interface AuthService {

    RegistrationResponse register(RegisterRequest request);

    IssuedSession verifyEmail(VerifyEmailRequest request, String userAgent, String ipAddress);

    void resendVerificationCode(String email);

    IssuedSession login(LoginRequest request, String userAgent, String ipAddress);

    IssuedSession refresh(String rawRefreshToken, String userAgent, String ipAddress);

    void logout(String rawRefreshToken);

    UserView currentUser(UUID userId);
}
