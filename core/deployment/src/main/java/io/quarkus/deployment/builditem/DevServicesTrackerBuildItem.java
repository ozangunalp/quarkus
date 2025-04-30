package io.quarkus.deployment.builditem;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.devservices.runtime.DevServicesConfigTracker;

// Ideally we would have a unique build item for each processor/feature, but that would need a new KeyedBuildItem or FeatureBuildItem type
// Needs to be in core because DevServicesResultBuildItem is in core
public final class DevServicesTrackerBuildItem extends SimpleBuildItem {

    // A map of a map of a list? What?!?
    // The reason this is like this, rather than being broken out into types, is because this map gets shared between classloaders, so language-level constructs work best
    private static Map<String, Map<Map, List<Closeable>>> systemInstance = null;
    private final DevServicesConfigTracker configTracker;

    public DevServicesTrackerBuildItem() {

        if (systemInstance == null) {
            try {
                Class s = ClassLoader.getSystemClassLoader().loadClass(DevServicesTrackerBuildItem.class.getName());
                systemInstance = (Map<String, Map<Map, List<Closeable>>>) s.getMethod("getBackingMap")
                        .invoke(null);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        configTracker = new DevServicesConfigTracker();

    }

    /**
     * Public so it can be used across classloaders. Should not be used except by this class.
     * Invoked reflectively.
     */
    public static Map getBackingMap() {
        if (systemInstance == null) {
            systemInstance = new ConcurrentHashMap<>();
        }
        return systemInstance;
    }

    public List getRunningServices(String featureName,
            Map config) {
        Map<Map, List<Closeable>> services = systemInstance.get(featureName);
        if (services != null) {
            return services.get(config);
        }
        return null;
    }

    public Set<Closeable> getAllServices(String featureName) {
        Map<Map, List<Closeable>> services = systemInstance.get(featureName);
        if (services == null) {
            return Set.of();
        } else {
            // Flatten
            Set<Closeable> ls = new HashSet<>();
            services.values().forEach(ls::addAll);
            return ls;
        }
    }

    public void addRunningService(String name, Map<String, String> identifyingConfig,
            DevServicesResultBuildItem.RunnableDevService service) {
        Map<Map, List<Closeable>> services = systemInstance.get(name);

        if (services == null) {
            services = new HashMap<>();
            systemInstance.put(name, services);
        }

        // Make a list so that we can add and remove to it
        List<Closeable> list = new ArrayList<>();
        list.add(service);
        services.put(identifyingConfig, list);

        configTracker.addRunningService(service);
    }

    public void removeRunningService(String name, Map<String, String> identifyingConfig,
            DevServicesResultBuildItem.RunnableDevService service) {
        Map<Map, List<Closeable>> services = systemInstance.get(name);

        if (services != null) {
            List servicesForConfig = services.get(identifyingConfig);
            if (servicesForConfig != null) {
                servicesForConfig.remove(service);
            }
        }

        configTracker.removeRunningService(service);

    }

}
