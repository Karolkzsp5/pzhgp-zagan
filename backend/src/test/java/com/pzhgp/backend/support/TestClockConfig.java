package com.pzhgp.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Podstawia {@link MutableClock} zamiast zegara systemowego w testach limitów zapytań. */
@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock();
    }
}
