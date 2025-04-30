package io.quarkus.devservices.deployment;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.quarkus.runtime.DevServicesTracker;

public class DevServicesConfigSource implements ConfigSource {

    DevServicesTracker devServiceTrackerBuildItem = new DevServicesTracker();

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }

    @Override
    public String getValue(String propertyName) {
        for (Object o : devServiceTrackerBuildItem.getAllRunningServices()) {
            Map config = (Map) ((Supplier) o).get();
            return (String) config.get(propertyName);

        }
        return null;
    }

    @Override
    public String getName() {
        return "DevServicesConfigSource";
    }

    @Override
    public int getOrdinal() {
        // greater than any default Microprofile ConfigSource
        return 500;
    }
}
