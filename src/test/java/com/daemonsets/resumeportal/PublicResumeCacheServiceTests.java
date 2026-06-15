package com.daemonsets.resumeportal;

import com.daemonsets.resumeportal.cache.PublicResumeCacheProperties;
import com.daemonsets.resumeportal.cache.PublicResumeCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PublicResumeCacheServiceTests {

    @Test
    void localCacheStoresAndEvictsPublicResumeDto() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PublicResumeCacheService cacheService = new PublicResumeCacheService(
                defaultProperties(),
                mock(StringRedisTemplate.class),
                new ObjectMapper(),
                meterRegistry
        );
        Map<String, Object> publicResume = publicResume();

        cacheService.put("token-1", publicResume);

        assertThat(cacheService.get("token-1")).contains(publicResume);
        assertThat(meterRegistry.counter(
                "resume.public.cache.lookup",
                "backend", "local",
                "outcome", "hit"
        ).count()).isEqualTo(1);

        cacheService.evict("token-1");

        assertThat(cacheService.get("token-1")).isEmpty();
    }

    @Test
    void disabledCacheDoesNotStorePublicResume() {
        PublicResumeCacheProperties properties = defaultProperties();
        properties.setEnabled(false);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PublicResumeCacheService cacheService = new PublicResumeCacheService(
                properties,
                mock(StringRedisTemplate.class),
                new ObjectMapper(),
                meterRegistry
        );

        cacheService.put("token-1", publicResume());

        assertThat(cacheService.get("token-1")).isEmpty();
        assertThat(meterRegistry.counter(
                "resume.public.cache.lookup",
                "backend", "none",
                "outcome", "bypassed"
        ).count()).isEqualTo(1);
    }

    private PublicResumeCacheProperties defaultProperties() {
        PublicResumeCacheProperties properties = new PublicResumeCacheProperties();
        properties.setBackend(PublicResumeCacheProperties.Backend.LOCAL);
        return properties;
    }

    private Map<String, Object> publicResume() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("firstName", "Alice");
        response.put("lastName", "Cached");
        response.put("designation", "Engineer");
        response.put("summary", "Cached resume");
        response.put("jobs", List.of());
        response.put("educations", List.of());
        response.put("skills", List.of("Java", "Redis"));
        response.put("theme", 1);
        return response;
    }
}
