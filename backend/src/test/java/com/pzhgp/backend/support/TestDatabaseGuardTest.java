package com.pzhgp.backend.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test Database Guard Unit Tests")
class TestDatabaseGuardTest {

    private final TestDatabaseGuard guard = new TestDatabaseGuard();

    @Test
    @DisplayName("Should allow a database whose name ends with _test")
    void allowsTestDatabase() {
        assertDoesNotThrow(() -> guard.initialize(contextWithUrl("jdbc:postgresql://localhost:5432/pzhgp_zagan_test")));
        assertDoesNotThrow(() -> guard.initialize(contextWithUrl("jdbc:postgresql://localhost:5432/pzhgp_zagan_test?sslmode=disable")));
    }

    @Test
    @DisplayName("Should stop the tests before they connect to the application database")
    void rejectsApplicationDatabase() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guard.initialize(contextWithUrl("jdbc:postgresql://localhost:5432/pzhgp_zagan")));

        assertTrue(ex.getMessage().contains("pzhgp_zagan"));
    }

    @Test
    @DisplayName("Should reject a missing address and a name that only contains _test in the middle")
    void rejectsMissingOrMisleadingAddress() {
        assertThrows(IllegalStateException.class, () -> guard.initialize(contextWithUrl(null)));
        assertThrows(IllegalStateException.class, () -> guard.initialize(contextWithUrl("jdbc:postgresql://localhost:5432/pzhgp_test_zagan")));
        assertThrows(IllegalStateException.class, () -> guard.initialize(contextWithUrl("jdbc:postgresql://localhost:5432/")));
    }

    @Test
    @DisplayName("Should read the database name without connection parameters")
    void readsDatabaseName() {
        assertEquals("pzhgp_zagan_test", TestDatabaseGuard.databaseName("jdbc:postgresql://db:5432/pzhgp_zagan_test?ssl=true"));
        assertEquals("pzhgp_zagan", TestDatabaseGuard.databaseName("jdbc:postgresql://localhost/pzhgp_zagan"));
        assertNull(TestDatabaseGuard.databaseName("jdbc:postgresql:pzhgp_zagan"));
        assertNull(TestDatabaseGuard.databaseName(null));
    }

    private static GenericApplicationContext contextWithUrl(String url) {
        MockEnvironment environment = new MockEnvironment();
        if (url != null) {
            environment.setProperty("spring.datasource.url", url);
        }

        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(environment);
        return context;
    }
}
