package com.riskscoring.gateway.service;

import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.model.EmailCodePurpose;

public interface EmailVerificationService {

    void issueAndSend(AppUser user, EmailCodePurpose purpose);

    void resend(AppUser user, EmailCodePurpose purpose);

    void verify(AppUser user, String code, EmailCodePurpose purpose);
}
