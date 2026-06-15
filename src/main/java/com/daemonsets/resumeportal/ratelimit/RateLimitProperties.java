package com.daemonsets.resumeportal.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;

    private Duration window = Duration.ofMinutes(1);

    private int loginCapacity = 20;

    private int publicCapacity = 300;

    private int pdfCapacity = 30;

    private int apiCapacity = 600;
}
