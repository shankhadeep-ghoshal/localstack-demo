package com.shankhadeepghoshal.chatapp.configs;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Factory
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
@SuppressWarnings({
    "PMD.ShortVariable",
    "PMD.UncommentedEmptyMethodBody",
    "PMD.DoNotUseThreads",
    "PMD.AvoidThreadGroup"
})
public class ThreadPoolFactory {
    transient ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(
                    10,
                    10,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    new MyCustomThreadFactory("STD-OUT-Pool-"));

    @Bean
    @Named("forConsoleOut")
    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    /** Same logic as the default thread factory of {@link ThreadPoolExecutor} */
    private static final class MyCustomThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final transient ThreadGroup group;
        private final transient AtomicInteger threadNumber = new AtomicInteger(1);
        private final transient String namePrefix;

        MyCustomThreadFactory(final String poolName) {
            group = Thread.currentThread().getThreadGroup();
            namePrefix =
                    String.join(
                            "", poolName, String.valueOf(poolNumber.getAndIncrement()), "-thread-");
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            t.setDaemon(false);
            return t;
        }
    }
}
