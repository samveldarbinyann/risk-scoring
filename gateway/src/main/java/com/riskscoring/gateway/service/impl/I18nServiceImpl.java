package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.service.I18nService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class I18nServiceImpl implements I18nService {

    private static final ResourceBundle.Control NO_DEFAULT_LOCALE_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private final String bundleBasename;
    private final Map<Locale, Map<String, String>> cache = new ConcurrentHashMap<>();

    public I18nServiceImpl(@Value("${spring.messages.basename}") String bundleBasename) {
        this.bundleBasename = bundleBasename.replace('/', '.');
    }

    @Override
    public Map<String, String> messagesFor(Locale locale) {
        return cache.computeIfAbsent(locale, this::loadMessages);
    }

    private Map<String, String> loadMessages(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(bundleBasename, locale, NO_DEFAULT_LOCALE_FALLBACK);
        return bundle.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), bundle::getString));
    }
}
