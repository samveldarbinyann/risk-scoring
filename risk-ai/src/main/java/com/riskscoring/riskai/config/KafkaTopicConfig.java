package com.riskscoring.riskai.config;

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
    public NewTopic signalsComputedTopic() {
        return TopicBuilder.name(Topics.SIGNALS_COMPUTED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic scanCompletedTopic() {
        return TopicBuilder.name(Topics.SCAN_COMPLETED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}
