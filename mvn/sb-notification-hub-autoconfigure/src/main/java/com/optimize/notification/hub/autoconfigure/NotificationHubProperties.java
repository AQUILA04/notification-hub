package com.optimize.notification.hub.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for Notification Hub client integration.
 *
 * <pre>
 * optimize:
 *   notification:
 *     hub:
 *       enabled: true
 *       base-url: http://localhost:8088
 *       tenant-id: demo-tenant   # optional; sent as X-Tenant-Id (local/dev)
 *       oauth2:
 *         enabled: true
 *         token-uri: http://localhost:8081/realms/notification-hub/protocol/openid-connect/token
 *         client-id: my-app
 *         client-secret: ${NOTIFICATION_HUB_CLIENT_SECRET}
 * </pre>
 */
@ConfigurationProperties(prefix = "optimize.notification.hub")
public class NotificationHubProperties {

    /**
     * Master switch. When false, no beans are registered.
     */
    private boolean enabled = true;

    /**
     * Base URL of the Notification Hub API (without trailing slash), e.g. {@code http://localhost:8088}.
     */
    private String baseUrl;

    /**
     * Optional tenant id sent as {@code X-Tenant-Id}. Useful for local profile when JWT is optional.
     * In production the tenant usually comes from the service-account JWT claim.
     */
    private String tenantId;

    /**
     * Connect timeout in milliseconds for Hub and token HTTP calls.
     */
    private int connectTimeoutMs = 5_000;

    /**
     * Read timeout in milliseconds for Hub and token HTTP calls.
     */
    private int readTimeoutMs = 30_000;

    private final OAuth2 oauth2 = new OAuth2();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public OAuth2 getOauth2() {
        return oauth2;
    }

    public static class OAuth2 {

        /**
         * When true, client-credentials tokens are fetched and sent as Bearer.
         */
        private boolean enabled = true;

        /**
         * Keycloak (or other) token endpoint.
         */
        private String tokenUri;

        private String clientId;

        private String clientSecret;

        /**
         * Refresh the cached token this many seconds before expiry.
         */
        private int refreshSkewSeconds = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public int getRefreshSkewSeconds() {
            return refreshSkewSeconds;
        }

        public void setRefreshSkewSeconds(int refreshSkewSeconds) {
            this.refreshSkewSeconds = refreshSkewSeconds;
        }
    }
}
