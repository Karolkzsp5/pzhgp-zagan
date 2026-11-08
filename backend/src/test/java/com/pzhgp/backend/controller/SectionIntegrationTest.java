package com.pzhgp.backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Section Integration Tests")
class SectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/sections - Should return sections in display order without logging in")
    void shouldReturnSectionsInDisplayOrder() throws Exception {
        mockMvc.perform(get("/api/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Żagań", "Wymiarki", "Chotków", "Kożuchów")))
                .andExpect(jsonPath("$[*].sortOrder").value(contains(1, 2, 3, 4)));
    }
}
