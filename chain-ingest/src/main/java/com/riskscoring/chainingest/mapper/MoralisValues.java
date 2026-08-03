package com.riskscoring.chainingest.mapper;

import com.riskscoring.chainingest.client.dto.MoralisTransaction;
import com.riskscoring.chainingest.exception.MoralisException;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Component
public class MoralisValues {

    private static final String FAILED_STATUS = "0";
    private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

    public Instant timestamp(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(iso.trim());
        } catch (DateTimeParseException e) {
            throw new MoralisException("Unparsable timestamp from Moralis: " + iso, e);
        }
    }

    public BigInteger wei(String value) {
        if (value == null || value.isBlank()) {
            return BigInteger.ZERO;
        }

        try {
            return new BigInteger(value.trim());
        } catch (NumberFormatException e) {
            throw new MoralisException("Unparsable wei value from Moralis: " + value, e);
        }
    }

    public String address(String address) {
        return address == null ? "" : address.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isRoutable(String address) {
        return !address.isEmpty() && !ZERO_ADDRESS.equals(address);
    }

    public boolean succeeded(MoralisTransaction transaction) {
        return !FAILED_STATUS.equals(transaction.receiptStatus());
    }
}
