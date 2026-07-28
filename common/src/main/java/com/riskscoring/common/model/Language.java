package com.riskscoring.common.model;

import java.util.Locale;

public enum Language {
    EN,
    RU;

    public static Language fromLocale(Locale locale) {
        return "ru".equalsIgnoreCase(locale.getLanguage()) ? RU : EN;
    }
}
