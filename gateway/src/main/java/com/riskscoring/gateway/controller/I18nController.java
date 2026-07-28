package com.riskscoring.gateway.controller;

import com.riskscoring.gateway.service.I18nService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/i18n")
@RequiredArgsConstructor
public class I18nController {

    private final I18nService i18nService;

    @GetMapping
    public Map<String, String> getMessages() {
        return i18nService.messagesFor(LocaleContextHolder.getLocale());
    }
}