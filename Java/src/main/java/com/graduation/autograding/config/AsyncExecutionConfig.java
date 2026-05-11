package com.graduation.autograding.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncExecutionConfig {

    @Bean(name = "judgeTaskExecutor")
    public Executor judgeTaskExecutor(
            @Value("${grading.judge-pool.core-size:2}") int corePoolSize,
            @Value("${grading.judge-pool.max-size:4}") int maxPoolSize,
            @Value("${grading.judge-pool.queue-capacity:200}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("judge-");
        executor.initialize();
        return executor;
    }
}
