package dev.gymbro.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.gymbro.common.web.RateLimitFilter;
import dev.gymbro.common.web.RateLimitProperties;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebConfig {

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            RateLimitProperties properties, ObjectMapper objectMapper) {

        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(properties, objectMapper));
        registration.addUrlPatterns("/api/auth/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("rateLimitFilter");
        return registration;
    }
}
