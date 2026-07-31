package com.riskscoring.gateway.service;

import com.riskscoring.gateway.dto.ContactRequest;

public interface ContactService {

    void submit(String clientIp, ContactRequest request);
}
