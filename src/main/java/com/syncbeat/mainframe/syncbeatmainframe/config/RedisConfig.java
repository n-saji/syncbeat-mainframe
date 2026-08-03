package com.syncbeat.mainframe.syncbeatmainframe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Container for dynamic per-room Redis Pub/Sub subscriptions. Topics are added/removed
 * at runtime by RoomChannelSubscriptionManager as clients subscribe/unsubscribe to
 * /topic/room/{roomId} — nothing is registered here at startup.
 */
@Configuration
public class RedisConfig {

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		return container;
	}
}
