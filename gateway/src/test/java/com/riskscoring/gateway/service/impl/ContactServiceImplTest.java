package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.dto.ContactRequest;
import com.riskscoring.gateway.entity.ContactSubmission;
import com.riskscoring.gateway.exception.EmailDeliveryException;
import com.riskscoring.gateway.exception.RateLimitExceededException;
import com.riskscoring.gateway.model.ContactStatus;
import com.riskscoring.gateway.repository.ContactSubmissionRepository;
import com.riskscoring.gateway.service.EmailService;
import com.riskscoring.gateway.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    private static final String CLIENT_IP = "203.0.113.10";

    @Mock
    private ContactSubmissionRepository contactSubmissionRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private RateLimitService rateLimitService;

    @Captor
    private ArgumentCaptor<ContactSubmission> submissionCaptor;

    private ContactServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ContactServiceImpl(contactSubmissionRepository, emailService, rateLimitService);
    }

    @Test
    void submitChecksRateLimitBeforeBuildingSubmission() {
        doThrow(new RateLimitExceededException(60)).when(rateLimitService).checkContact(CLIENT_IP);

        assertThatThrownBy(() -> service.submit(CLIENT_IP, request()))
                .isInstanceOf(RateLimitExceededException.class);

        verifyNoInteractions(emailService, contactSubmissionRepository);
    }

    @Test
    void submitStoresSubmissionAsSentWhenEmailDeliverySucceeds() {
        service.submit(CLIENT_IP, request());

        verify(contactSubmissionRepository).save(submissionCaptor.capture());
        ContactSubmission submission = submissionCaptor.getValue();
        assertThat(submission.getStatus()).isEqualTo(ContactStatus.SENT);
        assertThat(submission.getEmail()).isEqualTo("user@example.com");
        assertThat(submission.getIp()).isEqualTo(CLIENT_IP);
    }

    @Test
    void submitStoresSubmissionAsFailedWithoutThrowingWhenEmailDeliveryFails() {
        doThrow(new EmailDeliveryException(new RuntimeException("smtp down")))
                .when(emailService).sendContactNotification(org.mockito.ArgumentMatchers.any());

        service.submit(CLIENT_IP, request());

        verify(contactSubmissionRepository).save(submissionCaptor.capture());
        assertThat(submissionCaptor.getValue().getStatus()).isEqualTo(ContactStatus.FAILED);
    }

    private static ContactRequest request() {
        return new ContactRequest("user@example.com", "Question", "Hello there", UUID.randomUUID());
    }
}
