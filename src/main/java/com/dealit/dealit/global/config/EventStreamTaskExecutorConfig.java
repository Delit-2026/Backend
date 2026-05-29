package com.dealit.dealit.global.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class EventStreamTaskExecutorConfig {

	@Bean
	public Executor eventStreamTaskExecutor(
		@Value("${app.events.sse.executor.core-pool-size:4}") int corePoolSize,
		@Value("${app.events.sse.executor.max-pool-size:16}") int maxPoolSize,
		@Value("${app.events.sse.executor.queue-capacity:1000}") int queueCapacity
	) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("event-stream-");
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.initialize();
		return executor;
	}
}
