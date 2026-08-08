package com.riskscoring.paymentwatch.exception;

public class MoralisRateLimitException extends PaymentWatchException {

    public MoralisRateLimitException(String message) {
        super(message);
    }
}
