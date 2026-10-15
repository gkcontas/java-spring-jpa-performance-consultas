package com.gkcontas.queryperf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@code open-in-view: false}, which is what the application runs with and is not the
 * Spring Boot default.
 *
 * <p>The session is closed by the time the response is written, so a proxy the
 * serialiser reaches cannot be initialised and the request fails where anyone can see
 * it. Failing loudly is the point: with the setting on, the alternative is not "no
 * query", it is a query nobody can see.
 */
@Import(com.gkcontas.testsupport.LazyCarrierController.class)
class OpenInViewDisabledTest extends IntegrationTestBase {

    @Test
    void serialisingAnUninitialisedProxyFails() throws Exception {
        seedOnce();
        MvcResult result = mockMvc.perform(get("/test-only/lazy/1"))
                .andExpect(status().isInternalServerError())
                .andReturn();
        assertThat(result.getResolvedException())
                .isInstanceOf(org.springframework.http.converter.HttpMessageNotWritableException.class)
                .hasMessageContaining("Could not initialize proxy")
                .hasMessageContaining("no session");
    }
}
