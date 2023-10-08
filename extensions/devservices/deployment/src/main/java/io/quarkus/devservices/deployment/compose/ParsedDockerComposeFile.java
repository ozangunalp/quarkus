package io.quarkus.devservices.deployment.compose;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.testcontainers.images.ParsedDockerfile;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.google.common.collect.Sets;

/**
 * Representation of a docker-compose file, with partial parsing for validation and extraction of a minimal set of
 * data.
 */
public class ParsedDockerComposeFile {

    private static final Logger log = Logger.getLogger(ParsedDockerComposeFile.class);

    private final Map<String, Object> composeFileContent;

    private final String composeFileName;

    private final File composeFile;

    private final Map<String, Set<String>> serviceNameToImageNames = new HashMap<>();

    private final Map<String, Map<String, ?>> serviceNameToDefinition = new HashMap<>();

    public ParsedDockerComposeFile(File composeFile) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        try (FileInputStream fileInputStream = new FileInputStream(composeFile)) {
            composeFileContent = yaml.load(fileInputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse YAML file from " + composeFile.getAbsolutePath(), e);
        }
        this.composeFileName = composeFile.getAbsolutePath();
        this.composeFile = composeFile;
        parseAndValidate();
    }

    ParsedDockerComposeFile(Map<String, Object> testContent) {
        this.composeFileContent = testContent;
        this.composeFileName = "";
        this.composeFile = new File(".");

        parseAndValidate();
    }

    private void parseAndValidate() {
        final Map<String, ?> servicesMap;
        if (composeFileContent.containsKey("version")) {
            if ("2.0".equals(composeFileContent.get("version"))) {
                log.warnv(
                        "Testcontainers may not be able to clean up networks spawned using Docker Compose v2.0 files. " +
                                "Please see https://github.com/testcontainers/moby-ryuk/issues/2, and specify 'version: \"2.1\"' or "
                                +
                                "higher in {}",
                        composeFileName);
            }

            final Object servicesElement = composeFileContent.get("services");
            if (servicesElement == null) {
                log.debugv(
                        "Compose file {} has an unknown format: 'version' is set but 'services' is not defined",
                        composeFileName);
                return;
            }
            if (!(servicesElement instanceof Map)) {
                log.debugv("Compose file {} has an unknown format: 'services' is not Map", composeFileName);
                return;
            }

            servicesMap = (Map<String, ?>) servicesElement;
        } else {
            servicesMap = composeFileContent;
        }

        for (Map.Entry<String, ?> entry : servicesMap.entrySet()) {
            String serviceName = entry.getKey();
            Object serviceDefinition = entry.getValue();
            if (!(serviceDefinition instanceof Map)) {
                log.debugv(
                        "Compose file {} has an unknown format: service '{}' is not Map",
                        composeFileName,
                        serviceName);
                break;
            }

            final Map serviceDefinitionMap = (Map) serviceDefinition;

            validateNoContainerNameSpecified(serviceName, serviceDefinitionMap);
            findServiceImageName(serviceName, serviceDefinitionMap);
            findImageNamesInDockerfile(serviceName, serviceDefinitionMap);
            serviceNameToDefinition.put(serviceName, serviceDefinitionMap);
        }
    }

    private void validateNoContainerNameSpecified(String serviceName, Map serviceDefinitionMap) {
        if (serviceDefinitionMap.containsKey("container_name")) {
            throw new IllegalStateException(
                    String.format(
                            "Compose file %s has 'container_name' property set for service '%s' but this property is not supported by Testcontainers, consider removing it",
                            composeFileName,
                            serviceName));
        }
    }

    private void findServiceImageName(String serviceName, Map serviceDefinitionMap) {
        if (serviceDefinitionMap.containsKey("image") && serviceDefinitionMap.get("image") instanceof String) {
            final String imageName = (String) serviceDefinitionMap.get("image");
            log.debugv("Resolved dependency image for Docker Compose in {}: {}", composeFileName, imageName);
            serviceNameToImageNames.put(serviceName, Sets.newHashSet(imageName));
        }
    }

    private void findImageNamesInDockerfile(String serviceName, Map serviceDefinitionMap) {
        final Object buildNode = serviceDefinitionMap.get("build");
        Path dockerfilePath = null;

        if (buildNode instanceof Map) {
            final Map buildElement = (Map) buildNode;
            final Object dockerfileRelativePath = buildElement.get("dockerfile");
            final Object contextRelativePath = buildElement.get("context");
            if (dockerfileRelativePath instanceof String && contextRelativePath instanceof String) {
                dockerfilePath = composeFile
                        .getAbsoluteFile()
                        .getParentFile()
                        .toPath()
                        .resolve((String) contextRelativePath)
                        .resolve((String) dockerfileRelativePath)
                        .normalize();
            }
        } else if (buildNode instanceof String) {
            dockerfilePath = composeFile
                    .getAbsoluteFile()
                    .getParentFile()
                    .toPath()
                    .resolve((String) buildNode)
                    .resolve("./Dockerfile")
                    .normalize();
        }

        if (dockerfilePath != null && Files.exists(dockerfilePath)) {
            Set<String> resolvedImageNames = new ParsedDockerfile(dockerfilePath).getDependencyImageNames();
            if (!resolvedImageNames.isEmpty()) {
                log.debugv(
                        "Resolved Dockerfile dependency images for Docker Compose in {} -> {}: {}",
                        composeFileName,
                        dockerfilePath,
                        resolvedImageNames);
                this.serviceNameToImageNames.put(serviceName, resolvedImageNames);
            }
        }
    }

    public Map<String, Set<String>> getServiceNameToImageNames() {
        return serviceNameToImageNames;
    }

    public Map<String, ComposeServiceDefinition> getServiceDefinitions() {
        return serviceNameToDefinition.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> new ComposeServiceDefinition(e.getKey(), e.getValue())));
    }

}
