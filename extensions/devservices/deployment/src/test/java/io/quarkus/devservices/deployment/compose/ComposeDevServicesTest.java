package io.quarkus.devservices.deployment.compose;

import static io.quarkus.devservices.deployment.compose.ComposeDevServicesProcessor.isComposeFile;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class ComposeDevServicesTest {

    @Test
    void testComposeFile() {
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose.yml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose.yaml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose-my-service.yml")));
        assertTrue(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose-my-service.yaml")));
        assertFalse(isComposeFile(Path.of("/dev", "some", "dir", "docker-compose.txt")));
        assertFalse(isComposeFile(Path.of("/dev", "some", "dir", "my-service-docker-compose.yaml")));
    }
}
