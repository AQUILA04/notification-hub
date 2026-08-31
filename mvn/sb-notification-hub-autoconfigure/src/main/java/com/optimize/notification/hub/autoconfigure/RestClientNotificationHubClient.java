package com.optimize.notification.hub.autoconfigure;

import com.optimize.notification.hub.client.NotificationHubClient;
import com.optimize.notification.hub.client.NotificationHubClientException;
import com.optimize.notification.hub.model.Channel;
import com.optimize.notification.hub.model.CreateNotificationRequest;
import com.optimize.notification.hub.model.NotificationEventResponse;
import com.optimize.notification.hub.model.NotificationResponse;
import com.optimize.notification.hub.model.NotificationStatus;
import com.optimize.notification.hub.model.PageResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

final class RestClientNotificationHubClient implements NotificationHubClient {

    static final String TENANT_HEADER = "X-Tenant-Id";
    static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RestClient restClient;
    private final ClientCredentialsTokenProvider tokenProvider;
    private final String tenantId;

    RestClientNotificationHubClient(
            RestClient restClient,
            ClientCredentialsTokenProvider tokenProvider,
            String tenantId) {
        this.restClient = restClient;
        this.tokenProvider = tokenProvider;
        this.tenantId = tenantId;
    }

    @Override
    public NotificationResponse send(CreateNotificationRequest request) {
        return send(request, null);
    }

    @Override
    public NotificationResponse send(CreateNotificationRequest request, String idempotencyKey) {
        return restClient
                .post()
                .uri("/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(authAndTenantHeaders(idempotencyKey))
                .body(request)
                .retrieve()
                .onStatus(this::isError, this::mapError)
                .body(NotificationResponse.class);
    }

    @Override
    public NotificationResponse get(UUID id) {
        return restClient
                .get()
                .uri("/v1/notifications/{id}", id)
                .headers(authAndTenantHeaders(null))
                .retrieve()
                .onStatus(this::isError, this::mapError)
                .body(NotificationResponse.class);
    }

    @Override
    public PageResponse<NotificationResponse> list(
            NotificationStatus status, Channel channel, int page, int size) {
        return restClient
                .get()
                .uri(uriBuilder -> buildListUri(uriBuilder, status, channel, page, size))
                .headers(authAndTenantHeaders(null))
                .retrieve()
                .onStatus(this::isError, this::mapError)
                .body(new ParameterizedTypeReference<PageResponse<NotificationResponse>>() {});
    }

    @Override
    public List<NotificationEventResponse> events(UUID id) {
        return restClient
                .get()
                .uri("/v1/notifications/{id}/events", id)
                .headers(authAndTenantHeaders(null))
                .retrieve()
                .onStatus(this::isError, this::mapError)
                .body(new ParameterizedTypeReference<List<NotificationEventResponse>>() {});
    }

    @Override
    public OtpSendResponse sendOtp(OtpSendRequest request) {
        return sendOtp(request, null);
    }

    @Override
    public OtpSendResponse sendOtp(OtpSendRequest request, String idempotencyKey) {
        return restClient
                .post()
                .uri("/v1/otp/send")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(authAndTenantHeaders(idempotencyKey))
                .body(request)
                .retrieve()
                .onStatus(this::isError, this::mapError)
                .body(OtpSendResponse.class);
    }

    @Override
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        return restClient
                .post()
                .uri("/v1/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(authAndTenantHeaders(null))
                .body(request)
                .retrieve()
                .onStatus(this::isError, this::mapError)
                .body(OtpVerifyResponse.class);
    }

    private static java.net.URI buildListUri(
            UriBuilder uriBuilder,
            NotificationStatus status,
            Channel channel,
            int page,
            int size) {
        uriBuilder.path("/v1/notifications").queryParam("page", page).queryParam("size", size);
        if (status != null) {
            uriBuilder.queryParam("status", status.name());
        }
        if (channel != null) {
            uriBuilder.queryParam("channel", channel.name());
        }
        return uriBuilder.build();
    }

    private Consumer<HttpHeaders> authAndTenantHeaders(String idempotencyKey) {
        return headers -> {
            if (tokenProvider != null) {
                headers.setBearerAuth(tokenProvider.getAccessToken());
            }
            if (StringUtils.hasText(tenantId)) {
                headers.set(TENANT_HEADER, tenantId);
            }
            if (StringUtils.hasText(idempotencyKey)) {
                headers.set(IDEMPOTENCY_HEADER, idempotencyKey);
            }
        };
    }

    private boolean isError(org.springframework.http.HttpStatusCode status) {
        return status.isError();
    }

    private void mapError(
            org.springframework.http.HttpRequest request,
            org.springframework.http.client.ClientHttpResponse response)
            throws java.io.IOException {
        String body = new String(response.getBody().readAllBytes());
        throw new NotificationHubClientException(
                "Notification Hub call failed: "
                        + request.getMethod()
                        + " "
                        + request.getURI()
                        + " → HTTP "
                        + response.getStatusCode().value(),
                response.getStatusCode().value(),
                body);
    }
}
