package com.riskscoring.gateway.entity;

import com.riskscoring.gateway.model.ContactStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "contact_submission")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContactSubmission {

    @Id
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "scan_id")
    private UUID scanId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ContactStatus status;

    @Column(length = 64)
    private String ip;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContactSubmission submission)) {
            return false;
        }
        return id != null && id.equals(submission.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
