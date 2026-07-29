package com.riskscoring.gateway.service;

import com.riskscoring.gateway.entity.AppUser;

public interface EmailService {

    void sendVerificationCode(AppUser user, String code);
}