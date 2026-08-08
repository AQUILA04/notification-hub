package com.optimize.notification.hub.autoconfigure;

import com.optimize.notification.hub.client.NotificationHubClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationHubAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class, NotificationHubAutoConfiguration.class));

    @Test
    void doesNotCreateClientWithoutBaseUrl() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(NotificationHubClient.class));
    }

    @Test
    void createsClientWithOauth2Config() {
        contextRunner
                .withPropertyValues(
                        "optimize.notification.hub.base-url=http://localhost:8088",
                        "optimize.notification.hub.oauth2.token-uri=http://localhost:8081/token",
                        "optimize.notification.hub.oauth2.client-id=demo",
                        "optimize.notification.hub.oauth2.client-secret=secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationHubClient.class);
                    assertThat(context).hasSingleBean(ClientCredentialsTokenProvider.class);
                });
    }

    @Test
    void createsClientWithoutOauth2WhenTenantIdSet() {
        contextRunner
                .withPropertyValues(
                        "optimize.notification.hub.base-url=http://localhost:8088",
                        "optimize.notification.hub.oauth2.enabled=false",
                        "optimize.notification.hub.tenant-id=demo-tenant")
                .run(context -> {
                    assertThat(context).hasSingleBean(NotificationHubClient.class);
                    assertThat(context).doesNotHaveBean(ClientCredentialsTokenProvider.class);
                });
    }

    @Test
    void canBeDisabled() {
        contextRunner
                .withPropertyValues(
                        "optimize.notification.hub.enabled=false",
                        "optimize.notification.hub.base-url=http://localhost:8088")
                .run(context -> assertThat(context).doesNotHaveBean(NotificationHubClient.class));
    }
}
