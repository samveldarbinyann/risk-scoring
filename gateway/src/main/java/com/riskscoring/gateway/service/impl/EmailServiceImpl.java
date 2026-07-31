package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.config.GatewayProperties;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.entity.ContactSubmission;
import com.riskscoring.gateway.exception.EmailDeliveryException;
import com.riskscoring.gateway.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String VERIFICATION_TEMPLATE = "email/verification";
    private static final String CONTACT_TEMPLATE = "email/contact";
    private static final Locale CONTACT_LOCALE = Locale.ENGLISH;
    private static final DateTimeFormatter SUBMITTED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final GatewayProperties gatewayProperties;

    @Override
    public void sendVerificationCode(AppUser user, String code) {
        Locale locale = user.getLanguage().toLocale();
        Context context = new Context(locale, Map.of(
                "firstName", user.getFirstName(),
                "code", code,
                "ttlMinutes", gatewayProperties.verification().codeTtl().toMinutes()));

        send(user.getEmail(),
                messageSource.getMessage("email.verification.subject", null, locale),
                templateEngine.process(VERIFICATION_TEMPLATE, context));
    }

    @Override
    public void sendContactNotification(ContactSubmission submission) {
        Context context = new Context(CONTACT_LOCALE);
        context.setVariable("email", submission.getEmail());
        context.setVariable("subject", submission.getSubject());
        context.setVariable("message", submission.getMessage());
        context.setVariable("scanId", submission.getScanId());
        context.setVariable("submittedAt", SUBMITTED_AT_FORMAT.format(submission.getCreatedAt()));

        send(gatewayProperties.mail().contactRecipient(),
                messageSource.getMessage(
                        "email.contact.subject", new Object[]{submission.getSubject()}, CONTACT_LOCALE),
                templateEngine.process(CONTACT_TEMPLATE, context));
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
            log.info("Email sent to {}", mask(to));
        } catch (Exception exception) {
            log.error("Failed to send email to {}: {}", mask(to), exception.getMessage());
            throw new EmailDeliveryException(exception);
        }
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
