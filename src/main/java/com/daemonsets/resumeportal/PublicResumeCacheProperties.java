package com.daemonsets.resumeportal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.public-resume-cache")
public class PublicResumeCacheProperties {

    private boolean enabled = true;

    private Backend backend = Backend.LOCAL;

    private Duration ttl = Duration.ofMinutes(10);

    private long maximumSize = 10_000;

    public enum Backend {
        LOCAL,
        REDIS
    }
}
