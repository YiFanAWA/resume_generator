package com.daemonsets.resumeportal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class PublicResumeCacheService {

    private static final String KEY_PREFIX = "resume:public:";
    private static final TypeReference<Map<String, Object>> PUBLIC_RESUME_TYPE = new TypeReference<>() {
    };

    private final PublicResumeCacheProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Cache<String, Map<String, Object>> localCache;
    private final AtomicBoolean cacheFailureLogged = new AtomicBoolean(false);

    public PublicResumeCacheService(
            PublicResumeCacheProperties properties,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(properties.getMaximumSize())
                .expireAfterWrite(properties.getTtl())
                .build();
    }

    public Optional<Map<String, Object>> get(String shareToken) {
        if (!isUsableToken(shareToken) || !properties.isEnabled()) {
            recordLookup("none", "bypassed");
            return Optional.empty();
        }

        if (properties.getBackend() == PublicResumeCacheProperties.Backend.REDIS) {
            Optional<Map<String, Object>> redisValue = getFromRedis(shareToken);
            if (redisValue.isPresent()) {
                localCache.put(shareToken, redisValue.get());
                recordLookup("redis", "hit");
                return redisValue;
            }
            recordLookup("redis", "miss");
        }

        Optional<Map<String, Object>> localValue = Optional.ofNullable(localCache.getIfPresent(shareToken));
        recordLookup("local", localValue.isPresent() ? "hit" : "miss");
        return localValue;
    }

    public void put(String shareToken, Map<String, Object> publicResume) {
        if (!isUsableToken(shareToken) || !properties.isEnabled()) {
            return;
        }

        localCache.put(shareToken, publicResume);

        if (properties.getBackend() == PublicResumeCacheProperties.Backend.REDIS) {
            putToRedis(shareToken, publicResume);
        }
    }

    public void evict(String shareToken) {
        if (!isUsableToken(shareToken)) {
            return;
        }

        localCache.invalidate(shareToken);

        if (properties.isEnabled() && properties.getBackend() == PublicResumeCacheProperties.Backend.REDIS) {
            evictFromRedis(shareToken);
        }
    }

    private Optional<Map<String, Object>> getFromRedis(String shareToken) {
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey(shareToken));
            markCacheHealthy();
            if (!StringUtils.hasText(cached)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(cached, PUBLIC_RESUME_TYPE));
        } catch (JsonProcessingException | RuntimeException exception) {
            logCacheFailure("read", exception);
            return Optional.empty();
        }
    }

    private void putToRedis(String shareToken, Map<String, Object> publicResume) {
        try {
            String payload = objectMapper.writeValueAsString(publicResume);
            redisTemplate.opsForValue().set(cacheKey(shareToken), payload, properties.getTtl());
            markCacheHealthy();
        } catch (JsonProcessingException | RuntimeException exception) {
            logCacheFailure("write", exception);
        }
    }

    private void evictFromRedis(String shareToken) {
        try {
            redisTemplate.delete(cacheKey(shareToken));
            markCacheHealthy();
        } catch (RuntimeException exception) {
            logCacheFailure("evict", exception);
        }
    }

    private String cacheKey(String shareToken) {
        return KEY_PREFIX + shareToken;
    }

    private boolean isUsableToken(String shareToken) {
        return StringUtils.hasText(shareToken);
    }

    private void markCacheHealthy() {
        cacheFailureLogged.set(false);
    }

    private void logCacheFailure(String operation, Exception exception) {
        recordCacheFailure(operation);

        if (exception instanceof RedisConnectionFailureException || exception instanceof RedisSystemException) {
            if (cacheFailureLogged.compareAndSet(false, true)) {
                log.warn("Public resume Redis cache {} failed. Falling back to local cache/database. Cause: {}",
                        operation, exception.getMessage());
            } else {
                log.debug("Public resume Redis cache {} failed.", operation, exception);
            }
            return;
        }

        log.warn("Public resume cache {} failed. Falling back to database.", operation, exception);
    }

    private void recordLookup(String backend, String outcome) {
        Counter.builder("resume.public.cache.lookup")
                .description("Public resume cache lookup attempts")
                .tag("backend", backend)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    private void recordCacheFailure(String operation) {
        Counter.builder("resume.public.cache.failure")
                .description("Public resume cache operation failures")
                .tag("backend", "redis")
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
    }
}
