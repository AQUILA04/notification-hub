package com.optimize.notification.hub.client;

/**
 * Raised when a Notification Hub HTTP call fails.
 */
public class NotificationHubClientException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public NotificationHubClientException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public NotificationHubClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
