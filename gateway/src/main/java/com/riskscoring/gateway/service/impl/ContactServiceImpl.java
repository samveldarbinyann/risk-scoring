package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.dto.ContactRequest;
import com.riskscoring.gateway.entity.ContactSubmission;
import com.riskscoring.gateway.exception.EmailDeliveryException;
import com.riskscoring.gateway.model.ContactStatus;
import com.riskscoring.gateway.repository.ContactSubmissionRepository;
import com.riskscoring.gateway.service.ContactService;
import com.riskscoring.gateway.service.EmailService;
import com.riskscoring.gateway.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements ContactService {

    private final ContactSubmissionRepository contactSubmissionRepository;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;

    @Override
    public void submit(String clientIp, ContactRequest request) {
        rateLimitService.checkContact(clientIp);

        ContactSubmission submission = contactSubmissionRepository.save(ContactSubmission.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .subject(request.subject())
                .message(request.message())
                .scanId(request.scanId())
                .status(ContactStatus.RECEIVED)
                .ip(clientIp)
                .createdAt(Instant.now())
                .build());

        submission.setStatus(deliver(submission));
        contactSubmissionRepository.save(submission);
    }

    private ContactStatus deliver(ContactSubmission submission) {
        try {
            emailService.sendContactNotification(submission);
            log.info("Contact submission {} delivered", submission.getId());
            return ContactStatus.SENT;
        } catch (EmailDeliveryException exception) {
            log.error("Contact submission {} stored but not delivered", submission.getId(), exception);
            return ContactStatus.FAILED;
        }
    }
}
