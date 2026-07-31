package com.riskscoring.gateway.service;

import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.entity.ContactSubmission;

public interface EmailService {

    void sendVerificationCode(AppUser user, String code);

    void sendContactNotification(ContactSubmission submission);
}
