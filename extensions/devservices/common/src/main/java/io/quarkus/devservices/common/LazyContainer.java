package io.quarkus.devservices.common;

import java.io.IOException;
import java.net.ServerSocket;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.deployment.builditem.Startable;

public abstract class LazyContainer<SELF extends LazyContainer<SELF>> extends GenericContainer<SELF> implements
        Startable {

    public LazyContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    public void close() {
        // Trivial, but needed to avoid ambiguous inheritance
        super.close();
    }

    /**
     * Testcontainers docs: Because the randomised port mapping happens during container startup, the container must
     * be running at the time getMappedPort is called. You may need to ensure that the startup order of components
     * in your tests caters for this.
     *
     * We need the port before the container starts, so invent them ourselves
     */
    protected int findFreePort() {

        try (ServerSocket socket = new ServerSocket(0);) {
            // Make sure the socket becomes free as soon as we start the close operation
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
