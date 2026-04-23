package com.dealit.dealit.global.config;

import com.dealit.dealit.domain.chat.service.ChatSseRedisEventSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(ChatSseRedisEventSubscriber.class)
public class ChatSseRedisPubSubConfig {

    private final ChatSseRedisEventSubscriber subscriber;

    @Value("${app.chat.sse.redis.channel}")
    private String channel;

    @Bean
    public RedisMessageListenerContainer chatSseRedisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(channel));
        return container;
    }
}
