package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.service.I18nService;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class I18nServiceImpl implements I18nService {

    private static final String BUNDLE_BASENAME = "i18n.messages";

    private static final ResourceBundle.Control NO_DEFAULT_LOCALE_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    @Override
    public Map<String, String> messagesFor(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASENAME, locale, NO_DEFAULT_LOCALE_FALLBACK);
        return bundle.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), bundle::getString));
    }
}
