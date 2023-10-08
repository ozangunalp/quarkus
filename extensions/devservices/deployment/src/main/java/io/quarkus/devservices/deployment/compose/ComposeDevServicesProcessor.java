package io.quarkus.devservices.deployment.compose;

import static io.quarkus.devservices.common.Labels.DOCKER_COMPOSE_PROJECT_LABEL;
import static java.lang.Boolean.TRUE;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.github.dockerjava.api.model.ExposedPort;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.ComposeBuildTimeConfig;
import io.quarkus.deployment.dev.devservices.ComposeDevServicesBuildTimeConfig;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
public class ComposeDevServicesProcessor {

    private static final Logger log = Logger.getLogger(ComposeDevServicesProcessor.class);

    static final String PROJECT_PREFIX = "quarkus-compose-devservice";
    static final String LABEL_PREFIX = "io.quarkus.devservices.compose";
    static final String LABEL_IGNORE = LABEL_PREFIX + ".ignore";
    static final String LABEL_WAIT_FOR = LABEL_PREFIX + ".wait_for";
    static final String LABEL_WAIT_FOR_LOGS = LABEL_WAIT_FOR + ".logs";
    static final String LABEL_WAIT_FOR_PORTS = LABEL_WAIT_FOR + ".ports";
    static final String LABEL_WAIT_FOR_PORTS_DISABLE = LABEL_WAIT_FOR_PORTS + ".disable";
    static final String LABEL_WAIT_FOR_PORTS_TIMEOUT = LABEL_WAIT_FOR_PORTS + ".timeout";
    static final Pattern COMPOSE_FILE = Pattern.compile("(^docker-compose|^compose).*.(yml|yaml)");

    static volatile RunningDevService runningCompose;
    static volatile ComposeDevServiceCfg cfg;
    static volatile boolean first = true;

    @BuildStep
    public DevServicesComposeProjectBuildItem config(
            ComposeBuildTimeConfig composeBuildTimeConfig,
            ApplicationInfoBuildItem appInfo,
            LaunchModeBuildItem launchMode,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig,
            DockerStatusBuildItem dockerStatusBuildItem) throws IOException {

        ComposeDevServiceCfg configuration = new ComposeDevServiceCfg(composeBuildTimeConfig.devservices);

        if (runningCompose != null) {
            boolean shouldShutdownTheBroker = !configuration.equals(cfg);
            if (!shouldShutdownTheBroker) {
                return new DevServicesComposeProjectBuildItem(runningCompose.getConfig().get(DOCKER_COMPOSE_PROJECT_LABEL));
            }
            runningCompose.close();
            cfg = null;
        }

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Docker Compose Dev Services Starting:",
                consoleInstalledBuildItem, loggingSetupBuildItem, s -> s.getName().startsWith("ducttape")
                        || s.getName().startsWith("Thread"));
        try {
            runningCompose = startDockerCompose(configuration, appInfo.getName(),
                    dockerStatusBuildItem, devServicesConfig.timeout);
            if (runningCompose == null) {
                compressor.closeAndDumpCaptured();
            } else {
                compressor.close();
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (runningCompose == null) {
            return new DevServicesComposeProjectBuildItem();
        }

        if (first) {
            first = false;
            Runnable closeTask = () -> {
                if (runningCompose != null) {
                    try {
                        runningCompose.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                first = true;
                runningCompose = null;
                cfg = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }
        cfg = configuration;

        return new DevServicesComposeProjectBuildItem(runningCompose.getConfig().get(DOCKER_COMPOSE_PROJECT_LABEL));
    }

    private RunningDevService startDockerCompose(ComposeDevServiceCfg cfg,
            String appName,
            DockerStatusBuildItem dockerStatusBuildItem,
            Optional<Duration> timeout) {
        if (!cfg.devServicesEnabled) {
            // explicitly disabled
            log.debug("Not starting Compose dev services, as it has been disabled in the config.");
            return null;
        }

        if (cfg.files.isEmpty()) {
            // compose no files found
            log.debug("Could not find any compose files, not starting Compose dev services.");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Docker isn't working, not starting Compose dev services.");
            return null;
        }

        DockerComposeFiles dockerComposeFiles = new DockerComposeFiles(cfg.files);

        String identifier = PROJECT_PREFIX + "-" + appName + "-";
        ComposeContainer compose = new ComposeContainer(identifier, cfg.files)
                .withRemoveImages(cfg.removeImages)
                .withRemoveVolumes(cfg.removeVolumes)
                .withLocalCompose(cfg.useLocalCompose);
        // env vars
        compose.withEnv(cfg.envVariables);
        // options
        List<String> options = new ArrayList<>();
        options.add(cfg.profiles.stream().flatMap(p -> Stream.of("--profile", p)).collect(Collectors.joining(" ")));
        options.addAll(cfg.options);
        compose.withOptions(options.toArray(String[]::new));
        // timeout
        timeout.ifPresent(compose::withStartupTimeout);
        List<String> skippedServices = new ArrayList<>();
        // Iterate over service definitions
        for (ComposeServiceDefinition definition : dockerComposeFiles.getServiceDefinitions().values()) {
            String serviceName = definition.getServiceName();
            Map<String, Object> labels = definition.getLabels();
            // Skip service if contains ignore label
            if (labels.get(LABEL_IGNORE) == TRUE) {
                skippedServices.add(serviceName);
                continue;
            }
            // Skip the service if profiles doesn't match
            List<String> serviceProfiles = definition.getProfiles();
            if (!cfg.profiles.isEmpty() && serviceProfiles.stream().noneMatch(cfg.profiles::contains)) {
                continue;
            }
            // Add wait for health check
            if (definition.hasHealthCheck()) {
                compose.waitingFor(serviceName, Wait.forHealthcheck());
            } else {
                for (Map.Entry<String, Object> e : labels.entrySet()) {
                    // Add wait for log message
                    if (e.getKey().startsWith(LABEL_WAIT_FOR_LOGS)) {
                        int times = 1;
                        if (e.getKey().length() > LABEL_WAIT_FOR_LOGS.length()) {
                            try {
                                times = Integer.parseInt(e.getKey().replace(LABEL_WAIT_FOR_LOGS + ".", ""));
                            } catch (NumberFormatException t) {
                                log.warnv("Cannot parse label `{}`", e.getKey());
                            }
                        }
                        compose.waitingFor(serviceName, Wait.forLogMessage((String) e.getValue(), times));
                    }
                }
                // Add wait for port availability
                if (labels.get(LABEL_WAIT_FOR_PORTS_DISABLE) != TRUE) {
                    List<Integer> ports = definition.getPorts().stream().mapToInt(ExposedPort::getPort).boxed()
                            .collect(Collectors.toList());
                    ComposePortWaitStrategy waitStrategy = new ComposePortWaitStrategy(ports);
                    String waitForTimeout = (String) labels.get(LABEL_WAIT_FOR_PORTS_TIMEOUT);
                    if (waitForTimeout != null) {
                        waitStrategy.withStartupTimeout(Duration.parse("PT" + waitForTimeout));
                    }
                    compose.waitingFor(serviceName, waitStrategy);
                }
            }
        }
        // Run only non-skipped services
        if (!skippedServices.isEmpty()) {
            log.warnv("Skipping compose services {0}", skippedServices);
            Set<String> allServices = new HashSet<>(dockerComposeFiles.getAllServiceNames());
            skippedServices.forEach(allServices::remove);
            compose.withServices(allServices.toArray(String[]::new));
        }
        // Get project id from the compose client
        String projectId = getProjectIdFromCompose(compose, identifier);
        // Start compose
        compose.start();
        return new RunningDevService("docker-compose", null, compose::stop, DOCKER_COMPOSE_PROJECT_LABEL, projectId);
    }

    private String getProjectIdFromCompose(ComposeContainer compose, String fallbackIdentifier) {
        try {
            Field project = ComposeContainer.class.getDeclaredField("project");
            project.setAccessible(true);
            Object value = project.get(compose);
            return (String) value;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.warnv("Cannot access the Compose project name, using the prefix {0}", fallbackIdentifier);
            return fallbackIdentifier;
        }
    }

    static boolean isComposeFile(Path p) {
        return COMPOSE_FILE.matcher(p.getFileName().toString()).matches();
    }

    static List<File> filesFromConfigList(List<String> l) {
        return l.stream()
                .map(f -> Paths.get(f).toAbsolutePath().normalize())
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    static List<File> collectComposeFilesFromProjectRoot() throws RuntimeException {
        try {
            return Files.list(Paths.get(".").toAbsolutePath().normalize())
                    .filter(ComposeDevServicesProcessor::isComposeFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ComposeDevServiceCfg {
        private final boolean devServicesEnabled;
        private final List<File> files;
        private final List<String> profiles;
        private final List<String> options;
        private final boolean useLocalCompose;
        private final ComposeContainer.RemoveImages removeImages;
        private final boolean removeVolumes;
        private final Map<String, String> envVariables;

        public ComposeDevServiceCfg(ComposeDevServicesBuildTimeConfig cfg) {
            this.devServicesEnabled = cfg.enabled;
            this.files = cfg.files
                    .map(ComposeDevServicesProcessor::filesFromConfigList)
                    .orElseGet(ComposeDevServicesProcessor::collectComposeFilesFromProjectRoot);
            this.profiles = cfg.profiles.orElse(Collections.emptyList());
            this.options = cfg.options.orElse(Collections.emptyList());
            this.useLocalCompose = cfg.useLocalCompose;
            this.removeImages = ComposeContainer.RemoveImages.valueOf(cfg.removeImages.name());
            this.removeVolumes = cfg.removeVolumes;
            this.envVariables = cfg.envVariables;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ComposeDevServiceCfg that = (ComposeDevServiceCfg) o;
            return devServicesEnabled == that.devServicesEnabled
                    && Objects.equals(files, that.files)
                    && Objects.equals(profiles, that.profiles)
                    && Objects.equals(options, that.options)
                    && Objects.equals(useLocalCompose, that.useLocalCompose)
                    && Objects.equals(removeImages, that.removeImages)
                    && Objects.equals(removeVolumes, that.removeVolumes)
                    && Objects.equals(envVariables, that.envVariables);
        }

        @Override
        public int hashCode() {
            return Objects.hash(devServicesEnabled, files, profiles, options, useLocalCompose, removeImages, removeVolumes,
                    envVariables);
        }
    }
}
