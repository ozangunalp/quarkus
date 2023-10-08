package io.quarkus.deployment.dev.devservices;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "compose", phase = ConfigPhase.BUILD_TIME)
public class ComposeBuildTimeConfig {

    /**
     * Compose dev services config
     */
    @ConfigItem
    public ComposeDevServicesBuildTimeConfig devservices;
}
