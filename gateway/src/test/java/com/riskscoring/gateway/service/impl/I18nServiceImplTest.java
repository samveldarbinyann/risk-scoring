package com.riskscoring.gateway.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class I18nServiceImplTest {

    private final I18nServiceImpl service = new I18nServiceImpl("i18n/messages");

    @Test
    void messagesForReturnsEnglishBundleContent() {
        Map<String, String> messages = service.messagesFor(Locale.ENGLISH);

        assertThat(messages.get("error.scanNotFound")).isEqualTo("Scan not found: {0}");
    }

    @Test
    void messagesForReturnsRussianBundleContentForRussianLocale() {
        Map<String, String> messages = service.messagesFor(Locale.of("ru"));

        assertThat(messages.get("error.scanNotFound")).isEqualTo("Скан не найден: {0}");
    }

    @Test
    void messagesForCachesTheMapPerLocale() {
        Map<String, String> first = service.messagesFor(Locale.ENGLISH);
        Map<String, String> second = service.messagesFor(Locale.ENGLISH);

        assertThat(first).isSameAs(second);
    }
}
