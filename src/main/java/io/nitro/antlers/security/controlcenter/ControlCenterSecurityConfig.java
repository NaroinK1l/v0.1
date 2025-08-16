package io.nitro.antlers.security.controlcenter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Minimal Control Center security config stub.
 * <p>
 * This stub avoids compile-time dependency on Vaadin Control Center libraries.
 * It is only active on the "prod" profile and when running on Kubernetes.
 * Extend this class later if you actually deploy under Vaadin Control Center.
 */
@EnableWebSecurity
@Configuration
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
@Profile("prod")
public class ControlCenterSecurityConfig {
    // Intentionally empty
}
