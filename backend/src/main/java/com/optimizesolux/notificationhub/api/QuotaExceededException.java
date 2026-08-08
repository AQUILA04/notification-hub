package com.optimizesolux.notificationhub.api;

public class QuotaExceededException extends RuntimeException {

    private final String tenantId;
    private final String channel;
    private final String appId;
    private final long limit;
    private final long current;

    public QuotaExceededException(
            String tenantId, String channel, String appId, long limit, long current) {
        super(
                "Quota dépassé pour tenant="
                        + tenantId
                        + " canal="
                        + channel
                        + " app="
                        + appId
                        + " ("
                        + current
                        + "/"
                        + limit
                        + " req/min)");
        this.tenantId = tenantId;
        this.channel = channel;
        this.appId = appId;
        this.limit = limit;
        this.current = current;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getChannel() {
        return channel;
    }

    public String getAppId() {
        return appId;
    }

    public long getLimit() {
        return limit;
    }

    public long getCurrent() {
        return current;
    }
}
