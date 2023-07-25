package io.quarkus.virtual.threads;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class VirtualThreadsProcessor {

    @BuildStep
    void nativeRuntimeInitClasses(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitClasses) {
        runtimeInitClasses.produce(new RuntimeInitializedClassBuildItem(VirtualThreadExecutorSupplier.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void setupConfig(VirtualThreadsConfig config, VirtualThreadsRecorder recorder) {
        recorder.setConfig(config);
    }

}
