/**
 * @package com.nopaper.work.gateway.config -> gateway
 * @author saikatbarman
 * @date 2025 18-Oct-2025 3:38:49 pm
 * @git 
 */
package com.nopaper.work.gateway.config;

/**
 * 
 */

import java.time.Duration;
import java.util.concurrent.Executor;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


/**
 * Main configuration class for the API Gateway.
 * 
 * Configures:
 * - Redis caching with Jackson serialization
 * - Async task execution
 * - ObjectMapper for JSON processing
 * @param <RouteDefinitionDTO>
 */
@Configuration
@EnableCaching
@EnableAsync
public class GatewayConfiguration {
    
    /**
     * Configure ObjectMapper with Java 8 time support.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
    
    /**
     * Configure Redis cache manager with Jackson serialization.
     * This avoids NotSerializableException issues with complex objects.
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, 
                                     ObjectMapper objectMapper) {
        
        // Create JSON serializer with custom ObjectMapper
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // Configure default cache settings
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
            );
        
        // Configure cache-specific settings
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("routes", 
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("ip-blacklist", 
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30)))
            .build();
    }
    
    /**
     * Configure reactive Redis template for manual Redis operations.
     */
    @Bean("reactiveRedisTemplate") // Explicitly name the bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        // Key serializer
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        // Value serializer
        StringRedisSerializer valueSerializer = new StringRedisSerializer();

        RedisSerializationContext<String, String> context = RedisSerializationContext
            .<String, String>newSerializationContext(keySerializer)
            .value(valueSerializer)
            .hashKey(keySerializer)
            .hashValue(valueSerializer)
            .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
    
    /*
     * For caching DTOs or JSON values (recommended for complex objects):
     */
    /*
     * Removed the unused 'dtoRedisTemplate' bean for CustomRouteDefinitionDTO
     * to reduce confusion. The @Cacheable-based CacheManager is used instead.
     */
    /*
    @Bean
    public ReactiveRedisTemplate<String, CustomRouteDefinitionDTO> dtoRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<CustomRouteDefinitionDTO> valueSerializer =
            new Jackson2JsonRedisSerializer<>(CustomRouteDefinitionDTO.class);

        RedisSerializationContext<String, CustomRouteDefinitionDTO> context = RedisSerializationContext
            .<String, CustomRouteDefinitionDTO>newSerializationContext(keySerializer)
            .value(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
            .hashKey(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer))
            .hashValue(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
            .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
    */
    /**
     * Configure async task executor for @Async methods.
     * Used for asynchronous audit logging.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-audit-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
