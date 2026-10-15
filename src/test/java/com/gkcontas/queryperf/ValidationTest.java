package com.gkcontas.queryperf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import static org.assertj.core.api.Assertions.assertThat;

class ValidationTest extends IntegrationTestBase {

    /**
     * The contract is in English and so are the messages. Bean Validation's built-in
     * messages are resolved against the locale of the JVM, which on the machine this was
     * built on is pt-BR - every constraint in the project therefore writes its own.
     */
    @Test
    void rejectsAnImpossibleShapeWithAnEnglishMessage() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/dataset/custom")
                        .contentType("application/json")
                        .content("""
                                {"publishers":0,"authors":5,"books":4,
                                 "reviewsPerBook":2,"authorsPerBook":1,"coverRatio":1.0}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .contains("publishers must be at least 1");
    }

    @Test
    void rejectsACoverRatioAboveOne() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/dataset/custom")
                        .contentType("application/json")
                        .content("""
                                {"publishers":1,"authors":5,"books":4,
                                 "reviewsPerBook":2,"authorsPerBook":1,"coverRatio":1.5}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .contains("coverRatio must be at most 1.0");
    }
}
