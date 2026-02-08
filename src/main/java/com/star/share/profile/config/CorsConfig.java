package com.star.share.profile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration for the profile service.
 */
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Permit all origins (for development). In production, specify allowed origins for better security.
        config.setAllowedOriginPatterns(List.of("*"));
        // Permit specific HTTP methods. Adjust as needed based on your API design.
        config.setAllowedMethods(List.of("PATCH", "POST", "GET", "OPTIONS"));
        // Permit all headers. In production, specify allowed headers to enhance security.
        config.setAllowedHeaders(List.of("*"));
        // Not allow credentials (cookies, authorization headers, etc.)
        // to be included in cross-origin requests for security reasons.
        config.setAllowCredentials(false);
        // Set the maximum age (in seconds) for preflight requests to be cached by the browser.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply this CORS configuration to all endpoints under /api/v1/profile/. Adjust the path pattern as needed.
        source.registerCorsConfiguration("/api/v1/profile/**", config);
        return new CorsFilter(source);
    }
}
