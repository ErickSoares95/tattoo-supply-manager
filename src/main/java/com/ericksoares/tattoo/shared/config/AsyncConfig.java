package com.ericksoares.tattoo.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(persistOnRejection());

        // Let queued notifications finish on shutdown instead of dropping them.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * Queue full (or executor shutting down): instead of throwing TaskRejectedException and
     * losing the event, ask the task to persist itself so the scheduled retry picks it up.
     * Tasks that cannot do that keep the default abort behavior.
     */
    RejectedExecutionHandler persistOnRejection() {
        ThreadPoolExecutor.AbortPolicy fallback = new ThreadPoolExecutor.AbortPolicy();

        return (task, pool) -> {
            if (task instanceof RejectionAwareTask aware) {
                log.warn("Notification executor rejected a task (queue full or shutting down); persisting it for retry");
                aware.onRejected();
                return;
            }
            fallback.rejectedExecution(task, pool);
        };
    }

    @Bean(name = "aiExecutor")
    public Executor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-");

        executor.initialize();
        return executor;
    }
}
