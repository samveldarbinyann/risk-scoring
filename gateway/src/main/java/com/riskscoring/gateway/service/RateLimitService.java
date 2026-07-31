package com.riskscoring.gateway.service;

public interface RateLimitService {

    void checkPublicScan(String clientIp);
}
