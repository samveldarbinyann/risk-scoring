package com.riskscoring.common.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "targetType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AddressFacts.class, name = "ADDRESS"),
        @JsonSubTypes.Type(value = TransactionFacts.class, name = "TRANSACTION")
})
public sealed interface ChainFacts permits AddressFacts, TransactionFacts {
}
