package com.riskscoring.gateway.exception;

import com.riskscoring.gateway.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageSource);
    }

    @Test
    void handleValidationJoinsMultipleFieldErrorsWithSemicolon() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "email", "must not be blank"),
                new FieldError("request", "password", "must be at least 12 characters")));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().message())
                .isEqualTo("email: must not be blank; password: must be at least 12 characters");
    }

    @Test
    void handleValidationSingleFieldErrorHasNoSeparator() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "email", "must not be blank")));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(exception);

        assertThat(response.getBody().message()).isEqualTo("email: must not be blank");
    }

    @Test
    void handleUnreadableReturnsMalformedRequestResponse() {
        when(messageSource.getMessage(eq("error.malformedRequest"), any(), any())).thenReturn("Malformed request");
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("parse error");

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error()).isEqualTo("MALFORMED_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("Malformed request");
    }

    @Test
    void handleApiUsesStatusAndErrorCodeFromA4xxException() {
        UUID scanId = UUID.randomUUID();
        when(messageSource.getMessage(eq("error.scanNotFound"), any(), any())).thenReturn("Scan not found");

        ResponseEntity<ErrorResponse> response = handler.handleApi(new ScanNotFoundException(scanId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("SCAN_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Scan not found");
    }

    @Test
    void handleApiUsesStatusAndErrorCodeFromA5xxException() {
        when(messageSource.getMessage(eq("error.emailDeliveryFailed"), any(), any())).thenReturn("Email delivery failed");

        ResponseEntity<ErrorResponse> response = handler.handleApi(new EmailDeliveryException(new RuntimeException("smtp down")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().error()).isEqualTo("EMAIL_DELIVERY_FAILED");
        assertThat(response.getBody().message()).isEqualTo("Email delivery failed");
    }

    @Test
    void handleUnexpectedReturnsInternalServerErrorResponse() {
        when(messageSource.getMessage(eq("error.unexpected"), any(), any())).thenReturn("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Unexpected error");
    }
}
