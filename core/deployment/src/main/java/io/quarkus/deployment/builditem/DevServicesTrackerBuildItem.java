package io.quarkus.deployment.builditem;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesTracker;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesTracker.RunningDevService;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesTracker.ServiceConfig;

// Ideally we would have a unique build item for each processor/feature, but that would need a new KeyedBuildItem or FeatureBuildItem type
// Needs to be in core because DevServicesResultBuildItem is in core
public final class DevServicesTrackerBuildItem extends SimpleBuildItem {

    // This is a fairly thin wrapper around the tracker, so the tracker can be loaded with the system classloader
    // The QuarkusClassLoader takes care of loading the tracker with the right classloader
    private final RunningDevServicesTracker tracker;

    public DevServicesTrackerBuildItem() {
        tracker = new RunningDevServicesTracker();
    }

    public RunningDevServicesTracker tracker() {
        return tracker;
    }

    public List<RunningDevService> getRunningServices(ServiceConfig identifyingConfig) {
        return tracker.getRunningServices(identifyingConfig);
    }

    public List<RunningDevService> getRunningServicesWithDifferentConfig(ServiceConfig serviceConfig) {
        return tracker.getRunningServicesWithDifferentConfig(serviceConfig);
    }

}
