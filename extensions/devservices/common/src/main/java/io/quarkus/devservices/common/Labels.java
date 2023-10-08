package io.quarkus.devservices.common;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.datasource.common.runtime.DataSourceUtil;

public final class Labels {

    public static final String DOCKER_COMPOSE_PROJECT_LABEL = "com.docker.compose.project";
    private static final String DATASOURCE = "datasource";

    public static void addDataSourceLabel(GenericContainer<?> container, String datasourceName) {
        container.withLabel(DATASOURCE, DataSourceUtil.isDefault(datasourceName) ? "default" : datasourceName);
    }

    private Labels() {
    }
}
