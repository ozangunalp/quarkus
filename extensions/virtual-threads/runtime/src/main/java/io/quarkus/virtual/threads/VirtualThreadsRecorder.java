package io.quarkus.virtual.threads;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class VirtualThreadsRecorder {
    static VirtualThreadsConfig config = new VirtualThreadsConfig();

    public void setConfig(VirtualThreadsConfig c) {
        config = c;
    }
}
