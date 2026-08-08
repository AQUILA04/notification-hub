package com.optimizesolux.notificationhub.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PreviewTemplateRequest(@NotNull Map<String, Object> data) {}
