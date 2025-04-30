package io.quarkus.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

// Ideally we would have a unique build item for each processor/feature, but that would need a new KeyedBuildItem or FeatureBuildItem type
// Needs to be in core because DevServicesResultBuildItem is in core
public final class DevServicesConfigTracker {

    private static Set<Supplier<Map>> systemInstance = null;

    public DevServicesConfigTracker() {

        if (systemInstance == null) {
            try {
                Class s = ClassLoader.getSystemClassLoader().loadClass(DevServicesConfigTracker.class.getName());
                systemInstance = (Set<Supplier<Map>>) s.getMethod("getBackingSet")
                        .invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Public so it can be used across classloaders. Should not be used except by this class.
     * Invoked reflectively.
     */
    public static Set getBackingSet() {
        if (systemInstance == null) {
            systemInstance = new HashSet<>();
        }
        return systemInstance;
    }

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
