package com.riskscoring.riskai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RiskAiApplication {

    static void main(String[] args) {
        SpringApplication.run(RiskAiApplication.class, args);
    }
}
