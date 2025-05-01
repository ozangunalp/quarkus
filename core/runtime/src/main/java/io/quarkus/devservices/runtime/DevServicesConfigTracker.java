package io.quarkus.devservices.runtime;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

// Ideally we would have a unique build item for each processor/feature, but that would need a new KeyedBuildItem or FeatureBuildItem type
// Needs to be in core because DevServicesResultBuildItem is in core
public final class DevServicesConfigTracker {

    private static volatile Set<Supplier<Map>> systemInstance = null;

    public DevServicesConfigTracker() {
        //This needs to work across classloaders, but the QuarkusClassLoader will load us parent first
        if (systemInstance == null) {
            systemInstance = new HashSet<>();
        }
    }

    // This gets called an awful lot. Should we cache it?
    public Set<Supplier<Map>> getAllRunningServices() {
        return Collections.unmodifiableSet(systemInstance);
    }

    public void addRunningService(Supplier service) {
        systemInstance.add(service);
    }

    public void removeRunningService(Supplier service) {
        systemInstance.remove(service);
    }
}
