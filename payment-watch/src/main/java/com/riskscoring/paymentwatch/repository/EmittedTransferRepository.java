package com.riskscoring.paymentwatch.repository;

import com.riskscoring.paymentwatch.entity.EmittedTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmittedTransferRepository extends JpaRepository<EmittedTransfer, String> {
}
