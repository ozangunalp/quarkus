package io.quarkus.deployment.builditem;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.DevServicesTracker;

// Ideally we would have a unique build item for each processor/feature, but that would need a new KeyedBuildItem or FeatureBuildItem type
// Needs to be in core because DevServicesResultBuildItem is in core
public final class DevServicesTrackerBuildItem extends SimpleBuildItem {

    // A map of a map of a list? What?!?
    // The reason this is like this, rather than being broken out into types, is because this map gets shared between classloaders, so language-level constructs work best
    private final Map<String, Map<Map, List<Closeable>>> systemInstance;

    public DevServicesTrackerBuildItem() {

        systemInstance = new DevServicesTracker().getBackingMap();
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
    }

    public void removeRunningService(String name, Map<String, String> identifyingConfig,
            Closeable service) {
        Map<Map, List<Closeable>> services = systemInstance.get(name);

        if (services != null) {
            List servicesForConfig = services.get(identifyingConfig);
            if (servicesForConfig != null) {
                servicesForConfig.remove(service);
            }
        }
    }

}
