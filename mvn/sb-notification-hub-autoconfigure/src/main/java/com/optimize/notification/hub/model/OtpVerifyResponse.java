package com.optimize.notification.hub.model;

public record OtpVerifyResponse(boolean valid, OtpVerifyReason reason) {}
