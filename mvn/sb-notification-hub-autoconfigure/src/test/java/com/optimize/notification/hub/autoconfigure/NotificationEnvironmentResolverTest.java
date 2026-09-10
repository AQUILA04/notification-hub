package com.optimize.notification.hub.autoconfigure;

import com.optimize.notification.hub.model.NotificationEnvironment;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEnvironmentResolverTest {

    @Test
    void defaultsToTestWhenNoProdProfile() {
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("local", "dev");
        assertThat(NotificationEnvironmentResolver.resolve(env, null))
                .isEqualTo(NotificationEnvironment.TEST);
    }

    @Test
    void prodProfileResolvesProd() {
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("prod");
        assertThat(NotificationEnvironmentResolver.resolve(env, null))
                .isEqualTo(NotificationEnvironment.PROD);
    }

    @Test
    void productionProfileResolvesProd() {
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("docker", "production");
        assertThat(NotificationEnvironmentResolver.resolve(env, null))
                .isEqualTo(NotificationEnvironment.PROD);
    }

    @Test
    void configuredPropertyWinsOverProfile() {
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("prod");
        assertThat(NotificationEnvironmentResolver.resolve(env, "test"))
                .isEqualTo(NotificationEnvironment.TEST);
        assertThat(NotificationEnvironmentResolver.resolve(env, "prod"))
                .isEqualTo(NotificationEnvironment.PROD);
    }

    @Test
    void nullEnvironmentDefaultsToTest() {
        assertThat(NotificationEnvironmentResolver.resolve(null, null))
                .isEqualTo(NotificationEnvironment.TEST);
    }

    @Test
    void mockEnvironmentWithoutProfilesIsTest() {
        assertThat(NotificationEnvironmentResolver.resolve(new MockEnvironment(), "  "))
                .isEqualTo(NotificationEnvironment.TEST);
    }
}
