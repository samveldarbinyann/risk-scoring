package com.riskscoring.gateway.service;

import java.util.Locale;
import java.util.Map;

public interface I18nService {

    Map<String, String> messagesFor(Locale locale);
}