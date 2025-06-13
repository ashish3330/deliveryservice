package com.railswad.deliveryservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.railswad.deliveryservice.dto.StationDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    private final ObjectMapper objectMapper;

    public RedisConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration.builder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .usePooling()
                .build();
        return new JedisConnectionFactory(config, jedisClientConfiguration);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        return new StringRedisTemplate(jedisConnectionFactory);
    }

    @Bean
    public CacheManager cacheManager(JedisConnectionFactory jedisConnectionFactory) {
        // Create a Jackson2JsonRedisSerializer with ObjectMapper
        Jackson2JsonRedisSerializer<StationDTO> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, StationDTO.class);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(jedisConnectionFactory)
                .cacheDefaults(config)
                .build();
    }
}