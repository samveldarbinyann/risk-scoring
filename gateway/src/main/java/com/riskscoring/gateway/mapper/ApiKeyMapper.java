package com.riskscoring.gateway.mapper;

import com.riskscoring.gateway.dto.ApiKeyCreatedView;
import com.riskscoring.gateway.dto.ApiKeyView;
import com.riskscoring.gateway.entity.ApiKey;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyMapper {

    public ApiKeyView toView(ApiKey apiKey) {
        return new ApiKeyView(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                apiKey.getStatus(),
                apiKey.getLastUsedAt(),
                apiKey.getCreatedAt(),
                apiKey.getRevokedAt()
        );
    }

    public ApiKeyCreatedView toCreatedView(ApiKey apiKey, String plaintext) {
        return new ApiKeyCreatedView(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                plaintext,
                apiKey.getStatus(),
                apiKey.getCreatedAt()
        );
    }
}
