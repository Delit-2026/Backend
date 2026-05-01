package com.dealit.dealit.global.config;

import com.dealit.dealit.global.event.service.EventStreamRedisEventSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.events.sse.redis.enabled", havingValue = "true", matchIfMissing = true)
public class EventStreamRedisPubSubConfig {

    private final EventStreamRedisEventSubscriber subscriber;

    @Value("${app.events.sse.redis.channel}")
    private String channel;

    @Bean
    public RedisMessageListenerContainer eventStreamRedisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(channel));
        return container;
    }
}
