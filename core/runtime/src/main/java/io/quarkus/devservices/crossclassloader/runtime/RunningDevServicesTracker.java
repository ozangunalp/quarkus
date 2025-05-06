package io.quarkus.devservices.crossclassloader.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * * Note: This class should only use language-level classes and classes defined in this same package.
 * Other Quarkus classes might be in a different classloader.
 */
public class RunningDevServicesTracker {
    private static volatile Set<Supplier<Map>> configTracker = null;

    private static DevServicesIndexedByConfig servicesIndexedByFeature = null;

    public RunningDevServicesTracker() {
        //This needs to work across classloaders, but the QuarkusClassLoader will load us parent first
        if (configTracker == null) {
            configTracker = new HashSet<>();
        }
        if (servicesIndexedByFeature == null) {
            servicesIndexedByFeature = new DevServicesIndexedByConfig();
        }
    }

    // This gets called an awful lot. Should we cache it?
    public Set<Supplier<Map>> getConfigForAllRunningServices() {
        return Collections.unmodifiableSet(configTracker);
    }

    public List<RunningDevService> getRunningServices(AppConfig appConfig) {
        Map<ServiceConfig, List<RunningDevService>> servicesMap = servicesIndexedByFeature.get(appConfig);
        if (servicesMap == null) {
            return Collections.emptyList();
        }
        return servicesMap.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    public List<RunningDevService> getRunningServicesWithDifferentConfig(ServiceConfig serviceConfig) {
        Map<ServiceConfig, List<RunningDevService>> servicesMap = servicesIndexedByFeature.get(serviceConfig.appConfig);
        if (servicesMap == null) {
            return Collections.emptyList();
        }
        return servicesMap.entrySet().stream()
                .filter(entry -> !Objects.equals(entry.getKey(), serviceConfig))
                .flatMap(e -> e.getValue().stream())
                .toList();
    }

    public List<RunningDevService> getRunningServices(ServiceConfig identifyingConfig) {
        Map<ServiceConfig, List<RunningDevService>> servicesMap = servicesIndexedByFeature.get(identifyingConfig.appConfig);
        if (servicesMap == null) {
            return Collections.emptyList();
        }
        return servicesMap.getOrDefault(identifyingConfig, Collections.emptyList());
    }

    public void addRunningService(ServiceConfig identifyingConfig, RunningDevService service) {
        servicesIndexedByFeature
                .computeIfAbsent(identifyingConfig.appConfig, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(identifyingConfig, k -> new ArrayList<>())
                .add(service);
        configTracker.add((Supplier<Map>) service);
    }

    public void removeRunningService(ServiceConfig identifyingConfig, Closeable service) {
        Map<ServiceConfig, List<RunningDevService>> servicesMap = servicesIndexedByFeature.get(identifyingConfig.appConfig);

        if (servicesMap != null) {
            List<RunningDevService> runningDevServices = servicesMap.get(identifyingConfig);
            if (runningDevServices != null) {
                runningDevServices.remove(service);
                if (runningDevServices.isEmpty()) {
                    servicesMap.remove(identifyingConfig);
                }
            }
        }
        configTracker.remove(service);
    }

    /**
     * Type to give a bit of clarity of intent and avoid some of the thicket of angle brackets.
     * The key is a map of identifying config, and the value is a List of RunningDevService objects ... only they might be in a
     * different classloader, so we don't call them that.
     */
    private static class DevServicesIndexedByConfig
            extends ConcurrentHashMap<AppConfig, Map<ServiceConfig, List<RunningDevService>>> {
        public DevServicesIndexedByConfig() {
            super();
        }
    }

    public static class RunningDevService implements Closeable {

        protected final String name;
        protected final String description;
        protected final String containerId;
        protected final Map<String, String> config;
        protected final Closeable closeable;
        protected volatile boolean isRunning = true;

        public RunningDevService(String name, String description, String containerId, Closeable closeable,
                Map<String, String> config) {
            this.name = name;
            this.description = description;
            this.containerId = containerId;
            this.closeable = closeable;
            this.config = Collections.unmodifiableMap(config);
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getContainerId() {
            return containerId;
        }

        public Map<String, String> getConfig() {
            return config;
        }

        public Closeable getCloseable() {
            return closeable;
        }

        // This method should be on RunningDevService, but not on RunnableDevService, where we use different logic to
        // decide when it's time to close a container. For now, leave it where it is and hope it doesn't get called when it shouldn't.
        // We can either make a common parent class or throw unsupported when this is called from Runnable.
        public boolean isOwner() {
            return closeable != null;
        }

        @Override
        public void close() throws IOException {
            if (this.closeable != null) {
                this.closeable.close();
                isRunning = false;
            }
        }

        @Override
        public String toString() {
            return "RunningDevService{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", containerId='" + containerId + '\'' +
                    ", config=" + config +
                    ", closeable=" + closeable +
                    ", isRunning=" + isRunning +
                    '}';
        }
    }

    public static class RunnableDevService extends RunningDevService implements Supplier<Map<String, String>> {

        private final RunningDevServicesTracker tracker;
        private final Runnable container;
        private Map<String, Supplier> lazyConfig;
        private final ServiceConfig config;

        public RunnableDevService(String name,
                ServiceConfig config,
                String containerId,
                Runnable startable,
                Closeable closeable,
                Map lazyConfig,
                RunningDevServicesTracker tracker) {
            super(name, null, containerId, closeable, Map.of());
            this.config = config;
            this.container = startable;
            this.tracker = tracker;
            isRunning = false;
            this.lazyConfig = lazyConfig;
        }

        public boolean isRunning() {
            return isRunning;
        }

        public void start() {
            // We want to do two things; find things with the same config as us to reuse them, and find things with different config to close them
            // We figure out if we need to shut down existing redis containers that might have been started in previous profiles or restarts

            // These RunnableDevService classes could be from another classloader, so don't make assumptions about the class
            List<RunningDevService> runningServices = tracker.getRunningServices(config);
            if (runningServices.stream().anyMatch(r -> r.isRunning)) {
                // We have a match, so we can reuse this container
                return;
            }

            // Close running services that are not the same config
            List<RunningDevService> matchedDevServices = tracker.getRunningServicesWithDifferentConfig(config);
            for (RunningDevService devService : matchedDevServices) {
                try {
                    devService.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (container != null) {
                container.run();
                //  tell the tracker that we started
                tracker.addRunningService(config, this);
                isRunning = true;
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            tracker.removeRunningService(config, this);
        }

        @Override
        public Map<String, String> get() {
            // TODO printlns show this gets called way too often - does specifying the properties cut that down?
            Map config = getConfig();
            Map newConfig = new HashMap<>(config);
            for (Map.Entry<String, Supplier> entry : lazyConfig.entrySet()) {
                newConfig.put(entry.getKey(), entry.getValue().get());
            }
            return newConfig;
        }
    }

    public record ServiceConfig(AppConfig appConfig, Object config) {
    }

    public record AppConfig(String featureName,
            String launchMode,
            String devModeType,
            boolean auxiliaryApplication,
            String auxiliaryDevModeType,
            boolean test) {
    }

}
