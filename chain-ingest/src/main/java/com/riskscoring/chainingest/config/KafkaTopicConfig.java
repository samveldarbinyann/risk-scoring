package com.riskscoring.chainingest.config;

import com.riskscoring.common.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    @Bean
    public NewTopic scanRequestedTopic() {
        return TopicBuilder.name(Topics.SCAN_REQUESTED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic chainFetchedTopic() {
        return TopicBuilder.name(Topics.CHAIN_FETCHED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic scanProgressTopic() {
        return TopicBuilder.name(Topics.SCAN_PROGRESS)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}