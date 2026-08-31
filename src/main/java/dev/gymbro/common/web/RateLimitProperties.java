package dev.gymbro.common.web;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gymbro.rate-limit")
public record RateLimitProperties(Auth auth) {

    public record Auth(int capacity, Duration window) {
    }
}
