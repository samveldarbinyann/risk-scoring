package com.riskscoring.paymentwatch.exception;

public class PaymentWatchException extends RuntimeException {

    public PaymentWatchException(String message) {
        super(message);
    }

    public PaymentWatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
