package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.service.I18nService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/i18n")
@RequiredArgsConstructor
public class I18nController {

    private final I18nService i18nService;

    @GetMapping
    public Map<String, String> getMessages(@RequestParam(defaultValue = "en") String lang) {
        return i18nService.messagesFor(Locale.forLanguageTag(lang));
    }
}