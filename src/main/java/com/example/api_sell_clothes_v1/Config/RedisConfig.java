package com.example.api_sell_clothes_v1.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    // Lấy các giá trị từ application.properties
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}") // Giá trị mặc định là chuỗi rỗng nếu không có password
    private String redisPassword;

    @Value("${spring.data.redis.timeout}")
    private long redisTimeout;

    @Value("${spring.data.redis.database}")
    private int redisDatabase;

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        // Cấu hình Redis Standalone
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);

        // Nếu có password thì set password
        if (!redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        // Cấu hình timeout
        JedisClientConfiguration jedisClientConfig = JedisClientConfiguration.builder()
                .connectTimeout(Duration.ofMillis(redisTimeout))
                .readTimeout(Duration.ofMillis(redisTimeout))
                .build();

        // Tạo JedisConnectionFactory với cấu hình
        return new JedisConnectionFactory(redisConfig, jedisClientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());

        // Cấu hình serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}