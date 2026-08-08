package com.optimizesolux.notificationhub.api.dto;

import com.optimizesolux.notificationhub.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTemplateRequest(
        @NotBlank String name,
        @NotNull Channel channel,
        String subjectTemplate,
        @NotBlank String bodyTemplate
) {}
