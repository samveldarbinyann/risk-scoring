package com.riskscoring.gateway.entity;

import com.riskscoring.gateway.model.ContactStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContactSubmissionTest {

    @Test
    void instancesWithSameNonNullIdAreEqual() {
        UUID id = UUID.randomUUID();
        ContactSubmission a = submission(id);
        ContactSubmission b = submission(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void instancesWithDifferentIdsAreNotEqual() {
        ContactSubmission a = submission(UUID.randomUUID());
        ContactSubmission b = submission(UUID.randomUUID());

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void instancesWithNullIdAreNeverEqualToEachOther() {
        ContactSubmission a = submission(null);
        ContactSubmission b = submission(null);

        assertThat(a).isNotEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void isEqualToItself() {
        ContactSubmission a = submission(UUID.randomUUID());

        assertThat(a).isEqualTo(a);
    }

    @Test
    void isNotEqualToNullOrUnrelatedType() {
        ContactSubmission a = submission(UUID.randomUUID());

        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("not a ContactSubmission");
    }

    private static ContactSubmission submission(UUID id) {
        return ContactSubmission.builder()
                .id(id)
                .email("user@example.com")
                .subject("Question")
                .message("Hello")
                .status(ContactStatus.RECEIVED)
                .ip("203.0.113.10")
                .createdAt(Instant.now())
                .build();
    }
}
