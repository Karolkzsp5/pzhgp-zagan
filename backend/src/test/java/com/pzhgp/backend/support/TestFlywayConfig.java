package com.pzhgp.backend.support;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * W testach Flyway najpierw czyści bazę testową, a potem wykonuje wszystkie migracje od zera.
 * Każde uruchomienie testów zaczyna więc od pustej bazy i przy okazji sprawdza same migracje.
 * Klasa leży w pakiecie aplikacji, więc Spring dołącza ją do każdego kontekstu testowego.
 * Czyszczenie jest bezpieczne, bo TestDatabaseGuard dopuszcza tylko bazę z nazwą kończącą się na "_test".
 */
@Configuration(proxyBeanMethods = false)
public class TestFlywayConfig {

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
