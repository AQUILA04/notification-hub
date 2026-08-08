package com.optimizesolux.notificationhub.config;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String require() {
        String tenantId = CURRENT.get();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Missing tenant context");
        }
        return tenantId;
    }

    public static String getOrNull() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
