package io.quarkus.it.kotlin

import io.quarkus.scheduler.Scheduled
import io.smallrye.common.annotation.NonBlocking
import io.smallrye.mutiny.Multi
import io.smallrye.reactive.messaging.annotations.Merge
import io.vertx.core.Context
import io.vertx.mutiny.core.Vertx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Outgoing
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class Events(val vertx: Vertx, @Channel("test-out") val emitter: Emitter<Long>) {

    var items: MutableList<Long> = ArrayList()

    @Outgoing("test")
    fun produce(): Multi<Long> {
        println("${Thread.currentThread().name} ${Context.isOnVertxThread()} ${Vertx.currentContext()}")
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
            .onOverflow().drop()
            // TODO emit on vertx context
            // .emitOn { r -> vertx.runOnContext(r) }
            .onItem().invoke { _ -> println("${Thread.currentThread().name} ${Vertx.currentContext()} ${Thread.currentThread().javaClass.name} ${Context.isOnVertxThread()}")}
    }

    var counter = AtomicLong()

/*    @Channel("test")
    lateinit var testEmitter: Emitter<Long>

    // TODO emit on vertx context
    // @NonBlocking
    @Scheduled(every = "1s")
    fun produceEvery() {
        runBlocking {
            println("${Thread.currentThread().name} ${Context.isOnVertxThread()} ${Vertx.currentContext()}")
            testEmitter.send(counter.getAndIncrement())
        }
    }*/

    @Merge
    @Incoming("test")
    @Outgoing("mem")
    suspend fun consume(msg: Long): Long {
        delay(100)
        println("${Thread.currentThread().name} ${Context.isOnVertxThread()} ${Vertx.currentContext()} - ${Instant.now()} = $msg")
        return msg
    }

    @Incoming("mem")
    fun memConsume(msg: Long) {
        println("${Thread.currentThread().name} ${Context.isOnVertxThread()} ${Vertx.currentContext()} - mem = $msg")
        items.add(msg)
        emitter.send(msg)
    }

    fun items(): MutableList<Long> {
        return items
    }
}