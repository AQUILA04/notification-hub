package com.optimize.notification.hub.autoconfigure;

import com.optimize.notification.hub.model.NotificationEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Resolves the Notification Hub client environment from an explicit property, otherwise from
 * the active Spring Boot profile ({@code prod}/{@code production} → prod, anything else → test).
 */
public final class NotificationEnvironmentResolver {

    private NotificationEnvironmentResolver() {}

    public static NotificationEnvironment resolve(Environment environment, String configured) {
        if (StringUtils.hasText(configured)) {
            return NotificationEnvironment.from(configured);
        }
        String[] profiles = environment != null ? environment.getActiveProfiles() : new String[0];
        if (profiles.length == 0 && environment != null) {
            profiles = environment.getDefaultProfiles();
        }
        for (String profile : profiles) {
            if (profile != null
                    && (profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"))) {
                return NotificationEnvironment.PROD;
            }
        }
        return NotificationEnvironment.TEST;
    }
}
