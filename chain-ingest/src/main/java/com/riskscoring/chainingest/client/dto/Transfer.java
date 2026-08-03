package com.riskscoring.chainingest.client.dto;

import com.riskscoring.common.model.TransferDirection;

import java.math.BigInteger;
import java.time.Instant;

public record Transfer(
        String counterparty,
        TransferDirection direction,
        BigInteger valueNative,
        Instant at
) {
}
