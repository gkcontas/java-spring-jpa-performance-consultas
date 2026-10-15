package com.gkcontas.queryperf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The catalogue endpoints, which are the repairs in use rather than under measurement. */
class CatalogApiTest extends IntegrationTestBase {

    @BeforeEach
    void catalogue() {
        seedOnce();
    }

    @Test
    void servesAPageWithTheReviewCountOfEachBook() throws Exception {
        mockMvc.perform(get("/api/books").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].reviewCount").value(TEST_SHAPE.reviewsPerBook()))
                .andExpect(jsonPath("$[0].isbn").value("978-0000000001"));
    }

    @Test
    void servesAnEmptyListPastTheEnd() throws Exception {
        mockMvc.perform(get("/api/books").param("page", "500").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsAPageSizeOutsideTheAllowedRange() throws Exception {
        mockMvc.perform(get("/api/books").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/books").param("size", "500"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/books").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void servesTheSummariesOfTheWholeCatalogue() throws Exception {
        mockMvc.perform(get("/api/books/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(TEST_SHAPE.books()))
                .andExpect(jsonPath("$[0].publisherName").exists());
    }

    @Test
    void servesOneBookWithBothCollections() throws Exception {
        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authors.length()").value(TEST_SHAPE.authorsPerBook()))
                .andExpect(jsonPath("$.reviewCount").value(TEST_SHAPE.reviewsPerBook()))
                .andExpect(jsonPath("$.publisherName").exists())
                .andExpect(jsonPath("$.averageRating").exists());
    }

    @Test
    void answersNotFoundForAMissingBook() throws Exception {
        mockMvc.perform(get("/api/books/999999")).andExpect(status().isNotFound());
    }
}
