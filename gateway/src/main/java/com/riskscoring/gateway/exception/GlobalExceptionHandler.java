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

    @ExceptionHandler(ScanGroupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleScanGroupNotFound(ScanGroupNotFoundException exception) {
        String message = message("error.scanGroupNotFound", exception.getGroupId());
        return build(HttpStatus.NOT_FOUND, "SCAN_GROUP_NOT_FOUND", message);
    }

    @ExceptionHandler(ScanGroupReportNotReadyException.class)
    public ResponseEntity<ErrorResponse> handleScanGroupReportNotReady(ScanGroupReportNotReadyException exception) {
        String message = message("error.scanGroupReportNotReady", exception.getGroupId());
        return build(HttpStatus.CONFLICT, "SCAN_GROUP_REPORT_NOT_READY", message);
    }

    @ExceptionHandler(UnsupportedChainException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedChain(UnsupportedChainException exception) {
        String message = message("error.unsupportedChain", exception.getChainId(), EvmChain.supportedIds());
        return build(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CHAIN", message);
    }

    @ExceptionHandler(UnrecognizedAddressException.class)
    public ResponseEntity<ErrorResponse> handleUnrecognizedAddress(UnrecognizedAddressException exception) {
        String message = message("error.unrecognizedAddress", exception.getAddress());
        return build(HttpStatus.BAD_REQUEST, "UNRECOGNIZED_ADDRESS", message);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message("error.invalidCredentials"));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException exception) {
        String message = message("error.accountLocked", exception.getLockedUntil());
        return build(HttpStatus.LOCKED, "ACCOUNT_LOCKED", message);
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActive(AccountNotActiveException exception) {
        String message = message("error.accountNotActive", exception.getStatus());
        return build(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", message);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", message("error.invalidRefreshToken"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException exception) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message("error.unauthorized"));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return build(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", message("error.emailAlreadyRegistered"));
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyTaken(UsernameAlreadyTakenException exception) {
        return build(HttpStatus.CONFLICT, "USERNAME_ALREADY_TAKEN", message("error.usernameAlreadyTaken"));
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationCode(InvalidVerificationCodeException exception) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_CODE", message("error.invalidVerificationCode"));
    }

    @ExceptionHandler(VerificationCodeExpiredException.class)
    public ResponseEntity<ErrorResponse> handleVerificationCodeExpired(VerificationCodeExpiredException exception) {
        return build(HttpStatus.GONE, "VERIFICATION_CODE_EXPIRED", message("error.verificationCodeExpired"));
    }

    @ExceptionHandler(TooManyVerificationAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyAttempts(TooManyVerificationAttemptsException exception) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS", message("error.tooManyVerificationAttempts"));
    }

    @ExceptionHandler(ResendCooldownException.class)
    public ResponseEntity<ErrorResponse> handleResendCooldown(ResendCooldownException exception) {
        String message = message("error.resendCooldown", exception.getRetryAfterSeconds());
        return build(HttpStatus.TOO_MANY_REQUESTS, "RESEND_COOLDOWN", message);
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ErrorResponse> handleEmailDelivery(EmailDeliveryException exception) {
        log.error("Email delivery failed", exception);
        return build(HttpStatus.BAD_GATEWAY, "EMAIL_DELIVERY_FAILED", message("error.emailDeliveryFailed"));
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