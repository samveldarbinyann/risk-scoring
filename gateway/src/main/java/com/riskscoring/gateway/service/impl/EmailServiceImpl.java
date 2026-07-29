package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.EmailTemplateConfig;
import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.exception.EmailDeliveryException;
import com.riskscoring.gateway.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String VERIFICATION_TEMPLATE = "verification";

    private final JavaMailSender mailSender;
    private final TemplateEngine emailTemplateEngine;
    private final MessageSource messageSource;
    private final GatewayProperties gatewayProperties;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Qualifier(EmailTemplateConfig.EMAIL_TEMPLATE_ENGINE) TemplateEngine emailTemplateEngine,
                            MessageSource messageSource,
                            GatewayProperties gatewayProperties) {
        this.mailSender = mailSender;
        this.emailTemplateEngine = emailTemplateEngine;
        this.messageSource = messageSource;
        this.gatewayProperties = gatewayProperties;
    }

    @Override
    public void sendVerificationCode(AppUser user, String code) {
        Locale locale = user.getLanguage().toLocale();
        long ttlMinutes = gatewayProperties.verification().codeTtl().toMinutes();

        Map<String, Object> variables = Map.of(
                "title", message("email.verification.title", locale),
                "greeting", message("email.verification.greeting", locale, user.getFirstName()),
                "intro", message("email.verification.intro", locale),
                "code", code,
                "expiry", message("email.verification.expiry", locale, ttlMinutes),
                "security", message("email.verification.security", locale),
                "footer", message("email.verification.footer", locale)
        );

        send(user.getEmail(),
                message("email.verification.subject", locale),
                render(VERIFICATION_TEMPLATE, variables));
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(gatewayProperties.mail().from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Verification email sent to {}", mask(to));
        } catch (Exception exception) {
            log.error("Failed to send email to {}: {}", mask(to), exception.getMessage());
            throw new EmailDeliveryException(exception);
        }
    }

    private String render(String template, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return emailTemplateEngine.process(template, context);
    }

    private String message(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}