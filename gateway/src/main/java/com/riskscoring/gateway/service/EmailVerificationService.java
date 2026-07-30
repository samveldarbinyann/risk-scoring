package com.riskscoring.gateway.service;

import com.riskscoring.gateway.entity.AppUser;

public interface EmailVerificationService {

    void issueAndSend(AppUser user);

    void resend(AppUser user);

    void verify(AppUser user, String code);
}
