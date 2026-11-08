package com.pzhgp.backend.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Zegar testowy, który można przesunąć, żeby wcześniejsze próby wypadły poza okno limitu. */
public class MutableClock extends Clock {

    private volatile Instant now = Instant.parse("2026-01-01T00:00:00Z");

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
