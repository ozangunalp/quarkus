package io.quarkus.virtual.threads;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class VirtualThreadsConfig {

    /**
     * Whether virtual threads will be created with a name.
     * If {@code true} created virtual threads will have names prefixed with {{@link #prefix}}.
     */
    @ConfigItem(defaultValue = "false")
    boolean named = false;

    /**
     * Virtual thread name prefix, if {@link #named} is {@code true}
     */
    @ConfigItem(defaultValue = "quarkus-virtual-thread-")
    String prefix = "quarkus-virtual-thread-";
}
