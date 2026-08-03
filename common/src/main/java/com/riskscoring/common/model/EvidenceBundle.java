package com.riskscoring.common.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "targetType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AddressEvidence.class, name = "ADDRESS"),
        @JsonSubTypes.Type(value = TransactionEvidence.class, name = "TRANSACTION")
})
public sealed interface EvidenceBundle permits AddressEvidence, TransactionEvidence {

    String target();

    int chainId();

    Instant observedAt();
}
