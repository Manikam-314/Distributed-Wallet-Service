package com.programming.techie.agent.config;

import com.programming.techie.agent.memory.RedisChatMemory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * MemoryConfig — configures Redis-backed conversation memory.
 *
 * Uses RedisTemplate with Jackson serialization so Message objects
 * are stored as JSON in Redis with 30-minute TTL.
 * Survives container restarts and scales across multiple pods.
 */
@Configuration
public class MemoryConfig {

    @Bean
    public RedisTemplate<String, Object> chatMemoryRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ChatMemory chatMemory(RedisTemplate<String, Object> chatMemoryRedisTemplate) {
        return new RedisChatMemory(chatMemoryRedisTemplate);
    }
}
