package com.riskscoring.gateway.config;

import com.riskscoring.common.model.Language;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Arrays;

@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Language.EN.toLocale());
        resolver.setSupportedLocales(Arrays.stream(Language.values()).map(Language::toLocale).toList());
        return resolver;
    }
}