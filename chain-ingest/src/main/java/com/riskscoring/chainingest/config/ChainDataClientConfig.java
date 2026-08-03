package com.riskscoring.chainingest.config;

import com.riskscoring.chainingest.client.ChainDataClient;
import com.riskscoring.chainingest.client.ChainDataClientKey;
import com.riskscoring.chainingest.service.ChainFactsCacheService;
import com.riskscoring.common.model.ScanTarget;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class ChainDataClientConfig {

    @Bean
    public Map<ChainDataClientKey, ChainDataClient> chainDataClients(List<ChainDataClient> clients) {
        return clients.stream().collect(Collectors.toUnmodifiableMap(ChainDataClientKey::of, Function.identity()));
    }

    @Bean
    public Map<ScanTarget, ChainFactsCacheService> chainFactsCaches(List<ChainFactsCacheService> caches) {
        return caches.stream().collect(Collectors.toMap(
                ChainFactsCacheService::target,
                Function.identity(),
                (first, second) -> first,
                () -> new EnumMap<>(ScanTarget.class)));
    }
}
