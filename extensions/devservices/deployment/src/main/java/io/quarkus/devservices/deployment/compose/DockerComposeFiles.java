package io.quarkus.devservices.deployment.compose;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.testcontainers.utility.DockerImageName;

public class DockerComposeFiles {

    private final List<ParsedDockerComposeFile> parsedComposeFiles;

    public DockerComposeFiles(List<File> composeFiles) {
        this.parsedComposeFiles = composeFiles.stream().map(ParsedDockerComposeFile::new).collect(Collectors.toList());
    }

    public Set<String> getAllServiceNames() {
        return mergeServiceDependencyImageNames().keySet();
    }

    public Set<String> getDependencyImages() {
        Map<String, Set<String>> mergedServiceNameToImageNames = mergeServiceDependencyImageNames();

        return getImageNames(mergedServiceNameToImageNames);
    }

    private Map<String, Set<String>> mergeServiceDependencyImageNames() {
        Map<String, Set<String>> mergedServiceNameToImageNames = new HashMap<>();
        for (ParsedDockerComposeFile parsedComposeFile : parsedComposeFiles) {
            mergedServiceNameToImageNames.putAll(parsedComposeFile.getServiceNameToImageNames());
        }
        return mergedServiceNameToImageNames;
    }

    private Set<String> getImageNames(Map<String, Set<String>> serviceToImageNames) {
        return serviceToImageNames
                .values()
                .stream()
                .flatMap(Collection::stream)
                // Pass through DockerImageName to convert image names to canonical form (e.g. making implicit latest tag explicit)
                .map(DockerImageName::parse)
                .map(DockerImageName::asCanonicalNameString)
                .collect(Collectors.toSet());
    }

    public Map<String, ComposeServiceDefinition> getServiceDefinitions() {
        Map<String, ComposeServiceDefinition> mergedServiceDefinitions = new HashMap<>();
        for (ParsedDockerComposeFile parsedComposeFile : parsedComposeFiles) {
            mergedServiceDefinitions.putAll(parsedComposeFile.getServiceDefinitions());
        }
        return mergedServiceDefinitions;
    }
}
