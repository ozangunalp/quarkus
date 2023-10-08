package io.quarkus.devservices.deployment.compose;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import com.github.dockerjava.api.command.InspectContainerResponse;

public class ComposePortWaitStrategy extends HostPortWaitStrategy {
    private static final Logger log = Logger.getLogger(ComposePortWaitStrategy.class);

    static ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    List<Integer> ports;

    public ComposePortWaitStrategy(List<Integer> ports) {
        this.ports = ports;
    }

    @Override
    public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
        this.waitStrategyTarget = waitStrategyTarget;
        waitUntilReady();
    }

    @Override
    protected void waitUntilReady() {
        InspectContainerResponse containerInfo = waitStrategyTarget.getContainerInfo();
        this.waitStrategyTarget = new WaitStrategyTarget() {

            @Override
            public List<Integer> getExposedPorts() {
                return ports;
            }

            @Override
            public InspectContainerResponse getContainerInfo() {
                return containerInfo;
            }

            @Override
            public Integer getMappedPort(int originalPort) {
                Integer mappedPort = WaitStrategyTarget.super.getMappedPort(originalPort);
                return mappedPort;
            }
        };
        superWaitUntilReady();
    }

    protected void superWaitUntilReady() {
        final Set<Integer> externalLivenessCheckPorts;
        if (this.ports == null || this.ports.isEmpty()) {
            externalLivenessCheckPorts = getLivenessCheckPorts();
            if (externalLivenessCheckPorts.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debugv(
                            "Liveness check ports of {} is empty. Not waiting.",
                            waitStrategyTarget.getContainerInfo().getName());
                }
                return;
            }
        } else {
            externalLivenessCheckPorts = this.ports.stream()
                    .map(port -> waitStrategyTarget.getMappedPort(port))
                    .collect(Collectors.toSet());
        }

        List<Integer> exposedPorts = waitStrategyTarget.getExposedPorts();

        final Set<Integer> internalPorts = getInternalPorts(externalLivenessCheckPorts, exposedPorts);

        Callable<Boolean> externalCheck = new ExternalPortListeningCheck(
                waitStrategyTarget,
                externalLivenessCheckPorts);

        try {
            EXECUTOR.submit(
                    // Polling
                    () -> {
                        Instant now = Instant.now();
                        Awaitility
                                .await()
                                .pollInSameThread()
                                .pollInterval(Duration.ofMillis(100))
                                .pollDelay(Duration.ZERO)
                                .failFast("container is no longer running", () -> !waitStrategyTarget.isRunning())
                                .ignoreExceptions()
                                .forever()
                                .until(externalCheck);

                        log.debugv(
                                "External port check passed for {} mapped as {} in {}",
                                internalPorts,
                                externalLivenessCheckPorts,
                                Duration.between(now, Instant.now()));
                        return true;
                    }).get(startupTimeout.getSeconds(), TimeUnit.SECONDS);
        } catch (CancellationException | ExecutionException | TimeoutException e) {
            throw new ContainerLaunchException(
                    "Timed out waiting for container port to open (" +
                            waitStrategyTarget.getHost() +
                            " ports: " +
                            externalLivenessCheckPorts +
                            " should be listening)");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<Integer> getInternalPorts(Set<Integer> externalLivenessCheckPorts, List<Integer> exposedPorts) {
        return exposedPorts
                .stream()
                .filter(it -> externalLivenessCheckPorts.contains(waitStrategyTarget.getMappedPort(it)))
                .collect(Collectors.toSet());
    }

}
