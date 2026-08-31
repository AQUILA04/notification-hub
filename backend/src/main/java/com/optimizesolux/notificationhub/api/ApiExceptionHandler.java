package com.optimizesolux.notificationhub.api;

import com.optimizesolux.notificationhub.otp.api.OtpResendCooldownException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OtpResendCooldownException.class)
    ProblemDetail otpResendCooldown(OtpResendCooldownException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setTitle("Délai de renvoi OTP actif");
        problem.setProperty("code", "OTP_RESEND_COOLDOWN");
        problem.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        return problem;
    }

    @ExceptionHandler(OtpConfigurationException.class)
    ProblemDetail otpNotConfigured(OtpConfigurationException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Configuration OTP WhatsApp incomplète");
        problem.setProperty("code", "OTP_NOT_CONFIGURED");
        return problem;
    }

    @ExceptionHandler(ChannelNotEnabledException.class)
    ProblemDetail channelNotEnabled(ChannelNotEnabledException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Canal non disponible");
        problem.setProperty("channel", ex.getChannel().name());
        problem.setProperty("code", "CHANNEL_NOT_ENABLED");
        return problem;
    }

    @ExceptionHandler(QuotaExceededException.class)
    ProblemDetail quotaExceeded(QuotaExceededException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setTitle("Quota dépassé");
        problem.setProperty("code", "QUOTA_EXCEEDED");
        problem.setProperty("tenantId", ex.getTenantId());
        problem.setProperty("channel", ex.getChannel());
        problem.setProperty("appId", ex.getAppId());
        problem.setProperty("limit", ex.getLimit());
        problem.setProperty("current", ex.getCurrent());
        return problem;
    }

    @ExceptionHandler(CircuitOpenException.class)
    ProblemDetail circuitOpen(CircuitOpenException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Circuit ouvert");
        problem.setProperty("code", "CIRCUIT_OPEN");
        problem.setProperty("key", ex.getKey());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflictOrMissing(IllegalStateException ex) {
        HttpStatus status =
                ex.getMessage() != null && ex.getMessage().contains("tenant")
                        ? HttpStatus.BAD_REQUEST
                        : HttpStatus.CONFLICT;
        return ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {
        String detail =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(err -> err.getField() + ": " + err.getDefaultMessage())
                        .findFirst()
                        .orElse("Validation failed");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
