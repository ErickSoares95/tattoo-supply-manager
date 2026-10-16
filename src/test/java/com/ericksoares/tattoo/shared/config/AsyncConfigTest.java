package com.ericksoares.tattoo.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    private static final int MAX_POOL = 5;
    private static final int QUEUE = 100;

    /** Task that blocks until released, to saturate pool + queue. */
    private static class BlockingTask implements Runnable {
        private final CountDownLatch release;

        BlockingTask(CountDownLatch release) {
            this.release = release;
        }

        @Override
        public void run() {
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class AwareTask implements RejectionAwareTask {
        final AtomicInteger rejected = new AtomicInteger();
        final AtomicInteger ran = new AtomicInteger();

        @Override
        public void run() {
            ran.incrementAndGet();
        }

        @Override
        public void onRejected() {
            rejected.incrementAndGet();
        }
    }

    private void saturate(Executor executor, CountDownLatch release) {
        for (int i = 0; i < MAX_POOL + QUEUE; i++) {
            executor.execute(new BlockingTask(release));
        }
    }

    @Test
    void shouldPersistInsteadOfThrowingWhenQueueIsFull() {

        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().notificationExecutor();
        CountDownLatch release = new CountDownLatch(1);

        try {
            saturate(executor, release);

            AwareTask task = new AwareTask();

            assertDoesNotThrow(() -> executor.execute(task));

            assertEquals(1, task.rejected.get());
            assertEquals(0, task.ran.get());
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void shouldKeepAbortBehaviorForTasksThatCannotPersistThemselves() {

        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().notificationExecutor();
        CountDownLatch release = new CountDownLatch(1);

        try {
            saturate(executor, release);

            assertThrows(TaskRejectedException.class, () -> executor.execute(() -> { }));
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void shouldWaitForQueuedTasksOnShutdown() {

        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().notificationExecutor();

        // Verified through behavior: tasks accepted before shutdown still run.
        AwareTask task = new AwareTask();
        executor.execute(task);
        executor.shutdown();

        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
            while (task.ran.get() == 0) {
                Thread.sleep(10);
            }
        });
    }
}
