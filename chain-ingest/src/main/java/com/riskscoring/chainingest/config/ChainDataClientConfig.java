package com.riskscoring.chainingest.config;

import com.riskscoring.chainingest.client.ChainDataClient;
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
    public Map<ScanTarget, ChainDataClient> chainDataClients(List<ChainDataClient> clients) {
        return byTarget(clients, ChainDataClient::target);
    }

    @Bean
    public Map<ScanTarget, ChainFactsCacheService> chainFactsCaches(List<ChainFactsCacheService> caches) {
        return byTarget(caches, ChainFactsCacheService::target);
    }

    private <T> Map<ScanTarget, T> byTarget(List<T> beans, Function<T, ScanTarget> key) {
        return beans.stream().collect(Collectors.toMap(
                key, Function.identity(), (first, second) -> first, () -> new EnumMap<>(ScanTarget.class)));
    }
}
