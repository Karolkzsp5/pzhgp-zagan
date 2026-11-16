package com.pzhgp.backend.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Nie pozwala uruchomić testów na bazie innej niż testowa. W testach Hibernate usuwa i tworzy tabele
 * od nowa (create-drop), więc pomyłka w adresie mogłaby skasować prawdziwe dane. Sprawdzenie działa
 * przed połączeniem z bazą, dla każdego kontekstu Springa uruchamianego w testach.
 */
public class TestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String REQUIRED_SUFFIX = "_test";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String url = context.getEnvironment().getProperty("spring.datasource.url");
        String databaseName = databaseName(url);

        if (databaseName == null || !databaseName.endsWith(REQUIRED_SUFFIX)) {
            throw new IllegalStateException("Testy mogą działać tylko na osobnej bazie, której nazwa kończy się na \""
                    + REQUIRED_SUFFIX + "\" (np. pzhgp_zagan_test). Obecna baza: " + databaseName
                    + ". Ustaw TEST_DB_URL albo utwórz bazę pzhgp_zagan_test.");
        }
    }

    /** Nazwa bazy z adresu JDBC, np. pzhgp_zagan_test z jdbc:postgresql://localhost:5432/pzhgp_zagan_test?ssl=false. */
    static String databaseName(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }

        String withoutParameters = jdbcUrl.split("[?;]", 2)[0];
        int lastSlash = withoutParameters.lastIndexOf('/');
        return lastSlash < 0 ? null : withoutParameters.substring(lastSlash + 1);
    }
}
