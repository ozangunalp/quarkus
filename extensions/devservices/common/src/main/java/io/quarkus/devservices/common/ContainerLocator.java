package io.quarkus.devservices.common;

import static io.quarkus.devservices.common.Labels.DOCKER_COMPOSE_PROJECT_LABEL;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

import io.quarkus.runtime.LaunchMode;

public class ContainerLocator {

    private static final Logger log = Logger.getLogger(ContainerLocator.class);

    private static boolean hasLabels(Container container, Predicate<String> labelPredicate, String... devServiceLabels) {
        return Arrays.stream(devServiceLabels)
                .map(l -> container.getLabels().get(l))
                .anyMatch(labelPredicate);
    }

    private static final BiPredicate<ContainerPort, Integer> hasMatchingPort = (containerPort,
            port) -> containerPort.getPrivatePort() != null &&
                    containerPort.getPublicPort() != null &&
                    containerPort.getPrivatePort().equals(port);

    private final BiPredicate<Container, String> filter;
    private final int port;

    public ContainerLocator(String devServiceLabel, int port) {
        this.filter = (container, expectedLabel) -> expectedLabel.equals(container.getLabels().get(devServiceLabel));
        this.port = port;
    }

    public ContainerLocator(BiPredicate<Container, String> filter, int port) {
        this.filter = filter;
        this.port = port;
    }

    public static ContainerLocator locateContainerWithLabels(int port, String... devServiceLabels) {
        return new ContainerLocator(
                (container, expectedLabel) -> hasLabels(container, expectedLabel::equals, devServiceLabels),
                port);
    }

    public static ContainerLocator locateComposeContainerByImage(int port, String... imagePartials) {
        return new ContainerLocator(
                (container, expectedLabel) -> expectedLabel != null
                        && hasLabels(container, s -> s != null && s.contains(expectedLabel), DOCKER_COMPOSE_PROJECT_LABEL)
                        && Arrays.stream(imagePartials).anyMatch(partial -> container.getImage().contains(partial)),
                port);
    }

    private Stream<Container> lookup(String expectedLabelValue) {
        return DockerClientFactory.lazyClient().listContainersCmd().exec().stream()
                .filter(container -> filter.test(container, expectedLabelValue));
    }

    private Optional<ContainerPort> getMappedPort(Container container, int port) {
        return Arrays.stream(container.getPorts())
                .filter(containerPort -> hasMatchingPort.test(containerPort, port))
                .findAny();
    }

    public Optional<ContainerAddress> locateContainer(String serviceName, boolean shared, LaunchMode launchMode) {
        if (shared && launchMode == LaunchMode.DEVELOPMENT) {
            return lookup(serviceName)
                    .flatMap(container -> getMappedPort(container, port).stream()
                            .flatMap(containerPort -> Optional.ofNullable(containerPort.getPublicPort())
                                    .map(port -> {
                                        final ContainerAddress containerAddress = new ContainerAddress(
                                                container.getId(),
                                                DockerClientFactory.instance().dockerHostIpAddress(),
                                                containerPort.getPublicPort());
                                        log.infof("Dev Services container found: %s (%s). Connecting to: %s.",
                                                container.getId(),
                                                container.getImage(),
                                                containerAddress.getUrl());
                                        return containerAddress;
                                    }).stream()))
                    .findFirst();
        } else {
            return Optional.empty();
        }
    }

    public Optional<Integer> locatePublicPort(String serviceName, boolean shared, LaunchMode launchMode, int privatePort) {
        if (shared && launchMode == LaunchMode.DEVELOPMENT) {
            return lookup(serviceName)
                    .flatMap(container -> getMappedPort(container, privatePort).stream())
                    .findFirst()
                    .map(ContainerPort::getPublicPort);
        } else {
            return Optional.empty();
        }
    }
}
