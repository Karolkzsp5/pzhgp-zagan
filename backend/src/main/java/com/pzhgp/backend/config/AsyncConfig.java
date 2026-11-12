package com.pzhgp.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Włącza metody {@code @Async}. Wiadomości e-mail są wysyłane w tle, poza obsługą żądania. */
@Configuration
@EnableAsync
public class AsyncConfig {
}