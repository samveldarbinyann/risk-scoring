package com.riskscoring.paymentwatch.config;

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
    public NewTopic usdtPaymentDetectedTopic() {
        return TopicBuilder.name(Topics.USDT_PAYMENT_DETECTED)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}
