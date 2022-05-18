package io.quarkus.it.kotlin;

import static org.awaitility.Awaitility.await;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
class ReactiveMessagingKotlinTest {

    @Inject
    Events events;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Test
    void name() {
        //        companion.topics().create("messages", 1);
        companion.consume(Long.class).fromTopics("messages", 5)
                .awaitCompletion()
                .getRecords().stream().forEach(r -> System.out.println(r.value()));
        await().until(() -> events.getItems().size() >= 5);
    }
}
