package com.optimizesolux.notificationhub.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SmsTestRecipientRequest(@NotBlank @Email String email) {}
