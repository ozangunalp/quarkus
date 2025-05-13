package io.quarkus.deployment.builditem;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesTracker;
import io.quarkus.devservices.crossclassloader.runtime.RunningDevServicesTracker.RunnableDevService;

/**
 * BuildItem for running dev services.
 * Combines injected configs to the application with container id (if it exists).
 * <p>
 * Processors are expected to return this build item not only when the dev service first starts,
 * but also if a running dev service already exists.
 * <p>
 * {@link RunningDevService} helps to manage the lifecycle of the running dev service.
 */
public final class DevServicesResultBuildItem extends MultiBuildItem {

    private static final Logger log = Logger.getLogger(DevServicesResultBuildItem.class);

    private final String name;
    private final String description;
    private final String containerId;
    protected final Map<String, String> config;
    protected RunnableDevService runnableDevService;

    public static DevServicesResultBuildItem devServicesResult(RunningDevServicesTracker.RunningDevService service) {
        if (service instanceof RunnableDevService) {
            return new DevServicesResultBuildItem((RunnableDevService) service);
        }
        return new DevServicesResultBuildItem(service.getName(), service.getDescription(), service.getContainerId(),
                service.getConfig());
    }

    private DevServicesResultBuildItem(RunnableDevService service) {
        this(service.getName(), service.getDescription(), service.getContainerId(), service);
    }

    public DevServicesResultBuildItem(String name, String containerId, Map<String, String> config) {
        this(name, null, containerId, config);
    }

    public DevServicesResultBuildItem(String name, String description, String containerId, Map<String, String> config) {
        this.name = name;
        this.description = description;
        this.containerId = containerId;
        this.config = config;
    }

    public DevServicesResultBuildItem(String name, String description, String containerId,
            RunnableDevService runnableDevService) {
        this(name, description, containerId, Map.of());
        this.runnableDevService = runnableDevService;
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
        return getDynamicConfig();
    }

    public void start() {
        if (runnableDevService != null) {
            runnableDevService.start();
        } else {
            log.debugf("Not starting %s because runnable dev service is null (it is probably a running dev service.", name);
        }
    }

    // Ideally everyone would use the config source, but if people need to ask for config directly, make it possible
    public Map<String, String> getDynamicConfig() {
        if (runnableDevService != null && runnableDevService.isRunning()) {
            return runnableDevService.get();
        } else {
            return Collections.emptyMap();
        }
    }

    public static class RunningDevService implements Closeable {

        protected final String name;
        protected final String description;
        protected final String containerId;
        protected final Map<String, String> config;
        protected final Closeable closeable;
        protected volatile boolean isRunning = true;

        private static Map<String, String> mapOf(String key, String value) {
            Map<String, String> map = new HashMap<>();
            map.put(key, value);
            return map;
        }

        public RunningDevService(String name, String containerId, Closeable closeable, String key,
                String value) {
            this(name, null, containerId, closeable, mapOf(key, value));
        }

        public RunningDevService(String name, String description, String containerId, Closeable closeable, String key,
                String value) {
            this(name, description, containerId, closeable, mapOf(key, value));
        }

        public RunningDevService(String name, String containerId, Closeable closeable,
                Map<String, String> config) {
            this(name, null, containerId, closeable, config);
        }

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

        public DevServicesResultBuildItem toBuildItem() {
            return new DevServicesResultBuildItem(name, description, containerId, config);
        }
    }

}
