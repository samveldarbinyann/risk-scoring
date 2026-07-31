package com.riskscoring.gateway.repository;

import com.riskscoring.gateway.entity.ContactSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContactSubmissionRepository extends JpaRepository<ContactSubmission, UUID> {
}
