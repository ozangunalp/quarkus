package io.quarkus.virtual.threads;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

public enum VirtualThreadExecutorSupplier implements Supplier<Executor> {
    Instance;

    public Executor newVirtualThreadPerTaskExecutorWithName()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Method ofVirtual = Thread.class.getMethod("ofVirtual");
        Object vtb = ofVirtual.invoke(this);
        Class<?> vtbClass = Class.forName("java.lang.Thread$Builder$OfVirtual");
        Method name = vtbClass.getMethod("name", String.class, long.class);
        vtb = name.invoke(vtb, VirtualThreadsRecorder.config.prefix, 0);
        Method factory = vtbClass.getMethod("factory");
        ThreadFactory tf = (ThreadFactory) factory.invoke(vtb);

        return (Executor) Executors.class.getMethod("newThreadPerTaskExecutor", ThreadFactory.class)
                .invoke(this, tf);
    }

    public Executor newVirtualThreadPerTaskExecutor()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (Executor) Executors.class.getMethod("newVirtualThreadPerTaskExecutor")
                .invoke(this);
    }

    public Executor newVirtualThreadExecutor() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        try {
            return VirtualThreadsRecorder.config.named ? newVirtualThreadPerTaskExecutorWithName()
                    : newVirtualThreadPerTaskExecutor();
        } catch (ClassNotFoundException e) {
            logger.warn("Unable to invoke java.util.concurrent.Executors#newThreadPerTaskExecutor" +
                    " with VirtualThreadFactory, falling back to unnamed virtual threads", e);
            return newVirtualThreadPerTaskExecutor();
        }
    }

    private final Logger logger = Logger.getLogger(VirtualThreadExecutorSupplier.class);

    private final Executor executor;

    /**
     * This method uses reflection in order to allow developers to quickly test quarkus-loom without needing to
     * change --release, --source, --target flags and to enable previews.
     * Since we try to load the "Loom-preview" classes/methods at runtime, the application can even be compiled
     * using java 11 and executed with a loom-compliant JDK.
     */
    VirtualThreadExecutorSupplier() {
        Executor actual;
        try {
            var virtual = newVirtualThreadExecutor();
            actual = new Executor() {
                @Override
                public void execute(Runnable command) {
                    var context = Vertx.currentContext();
                    if (!(context instanceof ContextInternal)) {
                        virtual.execute(command);
                    } else {
                        ContextInternal contextInternal = (ContextInternal) context;
                        virtual.execute(new Runnable() {
                            @Override
                            public void run() {
                                final var previousContext = contextInternal.beginDispatch();
                                try {
                                    command.run();
                                } finally {
                                    contextInternal.endDispatch(previousContext);
                                }
                            }
                        });
                    }
                }
            };
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            logger.debug("Unable to invoke java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor", e);
            //quite ugly but works
            logger.warn("You weren't able to create an executor that spawns virtual threads, the default" +
                    " blocking executor will be used, please check that your JDK is compatible with " +
                    "virtual threads");
            //if for some reason a class/method can't be loaded or invoked we return the traditional executor,
            // wrapping executeBlocking.
            actual = new Executor() {
                @Override
                public void execute(Runnable command) {
                    var context = Vertx.currentContext();
                    if (!(context instanceof ContextInternal)) {
                        Infrastructure.getDefaultWorkerPool().execute(command);
                    } else {
                        context.executeBlocking(fut -> {
                            try {
                                command.run();
                                fut.complete(null);
                            } catch (Exception e) {
                                fut.fail(e);
                            }
                        }, false);
                    }
                }
            };
        }
        this.executor = actual;
    }

    @Override
    public Executor get() {
        return this.executor;
    }
}
