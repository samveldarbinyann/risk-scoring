package com.riskscoring.gateway.exception;

import com.riskscoring.common.model.EvmChain;
import com.riskscoring.gateway.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining("; "));

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", details);
    }

    @ExceptionHandler(UnsupportedChainException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedChain(UnsupportedChainException exception) {
        String message = message("error.unsupportedChain", exception.getChainId(), EvmChain.supportedIds());
        return build(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CHAIN", message);
    }

    @ExceptionHandler(ScanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleScanNotFound(ScanNotFoundException exception) {
        String message = message("error.scanNotFound", exception.getScanId());
        return build(HttpStatus.NOT_FOUND, "SCAN_NOT_FOUND", message);
    }

    @ExceptionHandler(ScanReportNotReadyException.class)
    public ResponseEntity<ErrorResponse> handleScanReportNotReady(ScanReportNotReadyException exception) {
        String message = message("error.scanReportNotReady", exception.getScanId(), exception.getStatus());
        return build(HttpStatus.CONFLICT, "REPORT_NOT_READY", message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message("error.unexpected"));
    }

    private String message(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(error, message, Instant.now()));
    }
}