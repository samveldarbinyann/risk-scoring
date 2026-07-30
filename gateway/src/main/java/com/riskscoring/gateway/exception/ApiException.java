package com.riskscoring.gateway.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String messageKey;
    private final Object[] messageArgs;

    protected ApiException(HttpStatus status, String errorCode, String messageKey, Object... messageArgs) {
        this(null, status, errorCode, messageKey, messageArgs);
    }

    protected ApiException(Throwable cause,
                           HttpStatus status,
                           String errorCode,
                           String messageKey,
                           Object... messageArgs) {
        super("%s %s".formatted(errorCode, Arrays.toString(messageArgs)), cause);
        this.status = status;
        this.errorCode = errorCode;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }
}
