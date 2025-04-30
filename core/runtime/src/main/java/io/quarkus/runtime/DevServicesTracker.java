package io.quarkus.runtime;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// Ideally we would have a unique build item for each processor/feature, but that would need a new KeyedBuildItem or FeatureBuildItem type
// Needs to be in core because DevServicesResultBuildItem is in core
public final class DevServicesTracker {

    // HACK! We share a backing map with the build item

    // A map of a map of a list? What?!?
    // The reason this is like this, rather than being broken out into types, is because this map gets shared between classloaders, so language-level constructs work best
    private volatile static Map<String, Map<Map, List<Closeable>>> systemInstance = null;

    public DevServicesTracker() {

        if (systemInstance == null) {
            try {
                Class s = ClassLoader.getSystemClassLoader().loadClass(DevServicesTracker.class.getName());
                systemInstance = (Map<String, Map<Map, List<Closeable>>>) s.getMethod("getBackingMap")
                        .invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Public so it can be used across classloaders. Should not be used except by this class, and by the
     * DevServicesTrackerBuildItem, which shares this map.
     * Invoked reflectively.
     */
    public static Map getBackingMap() {
        if (systemInstance == null) {
            systemInstance = new ConcurrentHashMap<>();
        }
        return systemInstance;
    }

    public Set<Supplier<Map>> getAllRunningServices() {
        Collection<Map<Map, List<Closeable>>> services = systemInstance.values();
        if (services == null) {
            return Set.of();
        } else {
            Set<Supplier<Map>> set = new HashSet<>();
            for (Map<Map, List<Closeable>> service : services) {
                // Flatten and recklessly cast, because the types needed for config do not match those needed for lifecycle management
                for (List<Closeable> list : service.values()) {
                    for (Closeable o : list) {
                        set.add((Supplier<Map>) o);
                    }
                }
            }
            return set;

        }
    }
}
