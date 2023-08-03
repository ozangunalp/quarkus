package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.PublisherDecorator;
import io.smallrye.reactive.messaging.providers.locals.LocalContextMetadata;

@ApplicationScoped
public class RequestScopedDecorator implements PublisherDecorator {
    @Override
    public Multi<? extends Message<?>> decorate(Multi<? extends Message<?>> publisher, String channelName,
            boolean isConnector) {
        if (isConnector) {
            return publisher.map(message -> {
                Optional<LocalContextMetadata> localContextMetadata = message.getMetadata(LocalContextMetadata.class);
                if (localContextMetadata.isPresent() && VertxContext.isOnDuplicatedContext()) {
                    ManagedContext requestContext = Arc.container().requestContext();
                    if (!requestContext.isActive()) {
                        requestContext.activate();
                        InjectableContext.ContextState state = requestContext.getState();
                        Runnable closeRequestContext = new Runnable() {
                            @Override
                            public void run() {
                                requestContext.destroy(state);
                                requestContext.deactivate();
                            }
                        };
                        Message<?> withAck = message.withAck(() -> message.ack()
                                .thenRun(closeRequestContext));
                        return withAck.withNack(throwable -> withAck.nack(throwable)
                                .thenRun(closeRequestContext));
                    }
                    return message;
                } else {
                    return message;
                }
            });
        }
        return publisher;
    }

    @Override
    public int getPriority() {
        return 100;
    }
}
