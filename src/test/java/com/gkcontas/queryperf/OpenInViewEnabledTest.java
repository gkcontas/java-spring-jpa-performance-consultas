package com.gkcontas.queryperf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * {@code open-in-view: true}, the Spring Boot default, and the reason an N+1 can live in
 * production for a year without a single test noticing.
 *
 * <p>The same request that fails with the setting off succeeds with it on: the session
 * is still open while the response is written, so the serialiser initialises the proxy
 * and the query happens in the view layer. Nothing in the service is wrong, nothing in
 * the repository is wrong, and no test of either of them can see it.
 */
@Import(com.gkcontas.testsupport.LazyCarrierController.class)
@SpringBootTest(properties = "spring.jpa.open-in-view=true")
class OpenInViewEnabledTest extends IntegrationTestBase {

    @Test
    void theSameRequestSucceedsAndHidesTheQuery() throws Exception {
        seedOnce();
        mockMvc.perform(get("/test-only/lazy/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publisherName").exists());
    }
}
