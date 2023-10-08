package io.quarkus.deployment.dev.devservices;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ComposeDevServicesBuildTimeConfig {

    /**
     * Docker compose dev service enabled or disabled
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * Docker compose dev service files
     */
    @ConfigItem
    public Optional<List<String>> files;

    /**
     * Docker compose dev service profiles
     */
    @ConfigItem
    public Optional<List<String>> profiles;

    /**
     * Docker compose dev service options
     */
    @ConfigItem
    public Optional<List<String>> options;

    /**
     * Docker compose dev service timeout
     */
    @ConfigItem(defaultValue = "5S")
    public Duration timeout;

    /**
     * Use local docker compose
     */
    @ConfigItem(defaultValue = "true")
    public boolean useLocalCompose;

    /**
     * Remove volumes on compose down
     */
    @ConfigItem(defaultValue = "false")
    public boolean removeVolumes;

    /**
     * Which images to remove on compose down
     */
    @ConfigItem(defaultValue = "local")
    public RemoveImages removeImages;

    /**
     * Copy of org.testcontainers.containers.ComposeContainer.RemoveImages to avoid unnecessary import
     */
    public enum RemoveImages {
        ALL,
        LOCAL
    }

    /**
     * Env variables to pass to all compose instances
     */
    @ConfigItem
    public Map<String, String> envVariables;
}
