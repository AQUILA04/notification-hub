package com.optimize.notification.hub.autoconfigure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimize.notification.hub.client.NotificationHubClientException;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Obtains and caches OAuth2 access tokens via client credentials.
 */
public class ClientCredentialsTokenProvider {

    private final NotificationHubProperties.OAuth2 oauth2;
    private final RestClient tokenClient;
    private final ObjectMapper objectMapper;

    private final Object lock = new Object();
    private volatile CachedToken cached;

    public ClientCredentialsTokenProvider(
            NotificationHubProperties properties, ObjectMapper objectMapper) {
        this.oauth2 = properties.getOauth2();
        this.objectMapper = objectMapper;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        this.tokenClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public String getAccessToken() {
        CachedToken current = cached;
        if (current != null && !current.isExpired(oauth2.getRefreshSkewSeconds())) {
            return current.accessToken();
        }
        synchronized (lock) {
            current = cached;
            if (current != null && !current.isExpired(oauth2.getRefreshSkewSeconds())) {
                return current.accessToken();
            }
            cached = fetchToken();
            return cached.accessToken();
        }
    }

    private CachedToken fetchToken() {
        if (!StringUtils.hasText(oauth2.getTokenUri())
                || !StringUtils.hasText(oauth2.getClientId())
                || !StringUtils.hasText(oauth2.getClientSecret())) {
            throw new NotificationHubClientException(
                    "OAuth2 is enabled but token-uri / client-id / client-secret are incomplete",
                    null);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", oauth2.getClientId());
        form.add("client_secret", oauth2.getClientSecret());

        try {
            String body = tokenClient
                    .post()
                    .uri(Objects.requireNonNull(oauth2.getTokenUri()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            (request, response) -> {
                                String err = new String(response.getBody().readAllBytes());
                                throw new NotificationHubClientException(
                                        "Failed to obtain access token from "
                                                + oauth2.getTokenUri()
                                                + ": HTTP "
                                                + response.getStatusCode().value(),
                                        response.getStatusCode().value(),
                                        err);
                            })
                    .body(String.class);

            JsonNode json = objectMapper.readTree(body);
            String accessToken = json.path("access_token").asText(null);
            if (!StringUtils.hasText(accessToken)) {
                throw new NotificationHubClientException(
                        "Token response missing access_token", -1, body);
            }
            long expiresIn = json.path("expires_in").asLong(300);
            return new CachedToken(accessToken, Instant.now().plusSeconds(expiresIn));
        } catch (NotificationHubClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new NotificationHubClientException("Failed to obtain access token", ex);
        }
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
        boolean isExpired(int skewSeconds) {
            return Instant.now().isAfter(expiresAt.minusSeconds(Math.max(0, skewSeconds)));
        }
    }
}
