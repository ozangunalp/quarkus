package io.quarkus.smallrye.reactivemessaging.runtime;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.EmitterType;

/**
 * Emitter implementation that plays better with context propagation
 *
 * @param <T> the payload type
 */
public interface ContextualEmitter<T> extends EmitterType {

    /**
     * Send a payload
     *
     * @param payload the payload
     * @return a Uni that completes when the message is sent
     */
    Uni<Void> send(T payload);

    /**
     * Send a payload and wait for the message to be sent
     *
     * @param payload the payload
     */
    void sendAndAwait(T payload);

    /**
     * Send a message
     *
     * @param msg the message
     * @return a Uni that completes when the message is sent
     * @param <M> the payload type
     */
    <M extends Message<? extends T>> Uni<Void> sendMessage(M msg);

    /**
     * Send a message and wait for the message to be sent
     *
     * @param msg the message
     * @param <M> the payload type
     */
    <M extends Message<? extends T>> void sendMessageAndAwait(M msg);
}
