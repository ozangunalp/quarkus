package io.quarkus.it.kotlin;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

@QuarkusIntegrationTest
@QuarkusTestResource(KafkaCompanionResource.class)
class ReactiveMessagingKotlinIT {

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Test
    void name() {
        companion.consume(Long.class).fromTopics("messages", 5)
                .awaitCompletion()
                .getRecords().stream().forEach(r -> System.out.println(r.value()));
    }
}
