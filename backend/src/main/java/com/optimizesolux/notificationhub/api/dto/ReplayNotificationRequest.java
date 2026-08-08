package com.optimizesolux.notificationhub.api.dto;

import jakarta.validation.constraints.NotNull;

public record ReplayNotificationRequest(
        @NotNull ReplayMode mode
) {
    public enum ReplayMode {
        /** Re-render / timeline only — no provider call */
        DRY_RUN,
        /** Re-enqueue a new send attempt */
        RESEND
    }
}
