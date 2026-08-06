package com.riskscoring.enrichment.client;

public class OfacSdnParseException extends RuntimeException {

    public OfacSdnParseException(Throwable cause) {
        super("Failed to parse OFAC SDN advanced XML", cause);
    }
}
