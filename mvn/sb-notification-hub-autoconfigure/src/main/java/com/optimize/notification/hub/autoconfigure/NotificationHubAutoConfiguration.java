package com.optimize.notification.hub.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimize.notification.hub.client.NotificationHubClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(NotificationHubProperties.class)
@ConditionalOnProperty(
        prefix = "optimize.notification.hub",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@ConditionalOnProperty(prefix = "optimize.notification.hub", name = "base-url")
public class NotificationHubAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "optimize.notification.hub.oauth2",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    ClientCredentialsTokenProvider notificationHubTokenProvider(
            NotificationHubProperties properties, ObjectMapper objectMapper) {
        Assert.hasText(
                properties.getOauth2().getTokenUri(),
                "optimize.notification.hub.oauth2.token-uri must be set when oauth2 is enabled");
        Assert.hasText(
                properties.getOauth2().getClientId(),
                "optimize.notification.hub.oauth2.client-id must be set when oauth2 is enabled");
        Assert.hasText(
                properties.getOauth2().getClientSecret(),
                "optimize.notification.hub.oauth2.client-secret must be set when oauth2 is enabled");
        return new ClientCredentialsTokenProvider(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationHubClient.class)
    NotificationHubClient notificationHubClient(
            NotificationHubProperties properties,
            ObjectProvider<ClientCredentialsTokenProvider> tokenProvider,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider) {
        Assert.hasText(properties.getBaseUrl(), "optimize.notification.hub.base-url must be set");

        if (properties.getOauth2().isEnabled()) {
            Assert.notNull(
                    tokenProvider.getIfAvailable(),
                    "OAuth2 is enabled but ClientCredentialsTokenProvider bean is missing");
        } else {
            Assert.hasText(
                    properties.getTenantId(),
                    "When oauth2 is disabled, optimize.notification.hub.tenant-id must be set (X-Tenant-Id)");
        }

        RestClient.Builder builder = restClientBuilderProvider.getIfAvailable(RestClient::builder);
        RestClient restClient = builder
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .requestFactory(requestFactory(properties))
                .build();

        return new RestClientNotificationHubClient(
                restClient,
                properties.getOauth2().isEnabled() ? tokenProvider.getObject() : null,
                properties.getTenantId());
    }

    private static ClientHttpRequestFactory requestFactory(NotificationHubProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return factory;
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return baseUrl;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
