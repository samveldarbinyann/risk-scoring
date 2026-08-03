package com.riskscoring.common.model;

public record TransactionFacts(
        TransactionSnapshot transaction
) implements ChainFacts {
}
