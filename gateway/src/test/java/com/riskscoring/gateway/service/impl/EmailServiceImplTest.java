package com.riskscoring.gateway.service.impl;

import com.riskscoring.common.model.Language;
import com.riskscoring.gateway.entity.AppUser;
import com.riskscoring.gateway.entity.ContactSubmission;
import com.riskscoring.gateway.exception.EmailDeliveryException;
import com.riskscoring.gateway.model.ContactStatus;
import com.riskscoring.gateway.model.UserRole;
import com.riskscoring.gateway.model.UserStatus;
import com.riskscoring.gateway.support.GatewayPropertiesFixture;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SpringTemplateEngine templateEngine;
    @Mock
    private MessageSource messageSource;

    private EmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailServiceImpl(mailSender, templateEngine, messageSource,
                GatewayPropertiesFixture.builder().mailFrom("noreply@example.com").build());
        lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("localized text");
        lenient().when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>body</html>");
        lenient().when(mailSender.createMimeMessage()).thenAnswer(invocation -> newMimeMessage());
    }

    @Test
    void sendVerificationCodeUsesRegistrationMessageKeysAndSendsToUser() throws Exception {
        AppUser user = user();
        when(messageSource.getMessage(eq("email.verification.subject"), any(), any())).thenReturn("Verify your email");

        service.sendVerificationCode(user, "123456");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertThat(sent.getSubject()).isEqualTo("Verify your email");
        assertThat(sent.getAllRecipients()[0].toString()).contains(user.getEmail());
        verify(templateEngine).process(eq("email/code-message"), any(Context.class));
    }

    @Test
    void sendPasswordResetCodeUsesPasswordResetMessageKeys() {
        AppUser user = user();
        when(messageSource.getMessage(eq("email.passwordReset.subject"), any(), any())).thenReturn("Reset your password");

        service.sendPasswordResetCode(user, "123456");

        verify(messageSource).getMessage(eq("email.passwordReset.title"), any(), any());
        verify(messageSource, org.mockito.Mockito.never()).getMessage(eq("email.verification.title"), any(), any());
    }

    @Test
    void sendContactNotificationSendsToConfiguredRecipientWithSanitizedSubject() throws Exception {
        ContactSubmission submission = ContactSubmission.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .subject("Hi\r\nBcc: evil@example.com")
                .message("Hello")
                .status(ContactStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();
        when(messageSource.getMessage(eq("email.contact.subject"), any(), any())).thenReturn("New contact request");

        service.sendContactNotification(submission);

        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(messageSource).getMessage(eq("email.contact.subject"), argsCaptor.capture(), any());
        String sanitizedSubjectArg = (String) argsCaptor.getValue()[0];
        assertThat(sanitizedSubjectArg).doesNotContain("\r").doesNotContain("\n");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getAllRecipients()[0].toString()).contains("contact@example.com");
    }

    @Test
    void sendWrapsMailSenderFailureInEmailDeliveryException() {
        org.mockito.Mockito.doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> service.sendVerificationCode(user(), "123456"))
                .isInstanceOf(EmailDeliveryException.class);
    }

    private static MimeMessage newMimeMessage() {
        return new MimeMessage(jakarta.mail.Session.getInstance(new Properties()));
    }

    private static AppUser user() {
        Instant now = Instant.now();
        return AppUser.builder()
                .id(UUID.randomUUID())
                .username("jane")
                .email("jane@example.com")
                .passwordHash("hash")
                .firstName("Jane")
                .lastName("Doe")
                .role(UserRole.USER)
                .status(UserStatus.PENDING_VERIFICATION)
                .language(Language.EN)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

}
