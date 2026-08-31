package com.optimizesolux.notificationhub.otp.api;

import com.optimizesolux.notificationhub.otp.api.dto.OtpSendRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpSendResponse;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyRequest;
import com.optimizesolux.notificationhub.otp.api.dto.OtpVerifyResponse;
import com.optimizesolux.notificationhub.otp.application.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Generates an OTP, stores it in Redis, and dispatches it on the chosen channel (WhatsApp or SMS).
     */
    @PostMapping("/send")
    public ResponseEntity<OtpSendResponse> send(
            @Valid @RequestBody OtpSendRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-App-Id", required = false) String appId) {
        OtpSendResponse response = otpService.send(request, idempotencyKey, appId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Verifies a submitted OTP code against the active session for the destination.
     */
    @PostMapping("/verify")
    public OtpVerifyResponse verify(@Valid @RequestBody OtpVerifyRequest request) {
        return otpService.verify(request);
    }
}
