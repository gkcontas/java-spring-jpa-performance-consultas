package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiTest extends IntegrationTestBase {

    /**
     * One of these tests seeds a different shape and another truncates, so the
     * catalogue is put back before each of them rather than shared. They assert on
     * counts, and JUnit does not promise an order.
     */
    @BeforeEach
    void freshCatalogue() {
        reseed();
    }

    @Test
    void listsEveryScenarioWithItsQuestion() throws Exception {
        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[*].question").exists());
    }

    @Test
    void runsOneScenarioAndStoresTheReport() throws Exception {
        mockMvc.perform(post("/api/scenarios/n-plus-one")
                        .param("warmups", "0").param("repeats", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenario").value("n-plus-one"))
                .andExpect(jsonPath("$.verdict").exists())
                .andExpect(jsonPath("$.variants[0].variant").value("lazy-loop"))
                .andExpect(jsonPath("$.variants[0].statements").value(books() + 1));

        mockMvc.perform(get("/api/scenarios/n-plus-one/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenario").value("n-plus-one"));
    }

    @Test
    void reportsEveryRecordedScenario() throws Exception {
        mockMvc.perform(delete("/api/measurements")).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/scenarios/projections")
                .param("warmups", "0").param("repeats", "1")).andExpect(status().isOk());
        mockMvc.perform(get("/api/measurements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void answersNotFoundForAnUnknownScenario() throws Exception {
        mockMvc.perform(post("/api/scenarios/no-such-thing"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/scenarios/no-such-thing/report"))
                .andExpect(status().isNotFound());
    }

    @Test
    void countsTheSeededCatalogue() throws Exception {
        mockMvc.perform(get("/api/dataset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").value(books()))
                .andExpect(jsonPath("$.reviews").value(reviews()));
    }

    @Test
    void seedsAndTruncatesThroughTheApi() throws Exception {
        mockMvc.perform(post("/api/dataset/custom")
                        .contentType("application/json")
                        .content("""
                                {"publishers":2,"authors":5,"books":4,
                                 "reviewsPerBook":2,"authorsPerBook":1,"coverRatio":1.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books").value(4))
                .andExpect(jsonPath("$.reviews").value(8));

        mockMvc.perform(delete("/api/dataset")).andExpect(status().isNoContent());
        assertThat(count("SELECT count(*) FROM book")).isZero();
    }

    private static int books() {
        return TEST_SHAPE.books();
    }

    private static int reviews() {
        return TEST_SHAPE.books() * TEST_SHAPE.reviewsPerBook();
    }
}
