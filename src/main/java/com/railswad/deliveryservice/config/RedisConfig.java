package com.railswad.deliveryservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("172.17.0.1"); // or VPS IP if remote
        config.setPort(6379);

        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration.builder()
                .connectTimeout(Duration.ofSeconds(5)) // connection timeout
                .readTimeout(Duration.ofSeconds(5))    // read timeout
                .usePooling()
                .build();

        return new JedisConnectionFactory(config, jedisClientConfiguration);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        return new StringRedisTemplate(jedisConnectionFactory);
    }
}
