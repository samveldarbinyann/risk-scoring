package com.riskscoring.chainingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ChainIngestApplication {

    static void main(String[] args) {
        SpringApplication.run(ChainIngestApplication.class, args);
    }
}