package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A marker build item that, if any instances are provided during the build, the containers started by DevServices
 * will use a shared network.
 */
public final class DevServicesComposeProjectBuildItem extends SimpleBuildItem {
    private final String project;

    public DevServicesComposeProjectBuildItem() {
        this(null);
    }

    public DevServicesComposeProjectBuildItem(String project) {
        this.project = project;
    }

    public String getProject() {
        return project;
    }
}
