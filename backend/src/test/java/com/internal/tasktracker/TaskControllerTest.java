package com.internal.tasktracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Protects against: SQL precedence bug where the description branch ignored
    // archived = FALSE, leaking the two archived "api" rows into results.
    @Test
    void searchExcludesArchivedTasks() throws Exception {
        mockMvc.perform(get("/api/tasks").param("q", "api").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].archived", everyItem(is(false))))
                .andExpect(jsonPath("$.items[*].title").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Legacy API cleanup"))));
    }

    // Protects against: the status filter being bypassed for title matches because
    // AND bound tighter than OR in the generated SQL. "fix" matches non-archived
    // DONE rows, so the assertion is not vacuous.
    @Test
    void statusFilterAppliesToEveryMatch() throws Exception {
        mockMvc.perform(get("/api/tasks").param("q", "fix").param("status", "DONE").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThan(0)))
                .andExpect(jsonPath("$.items[*].status", everyItem(is("DONE"))));
    }

    // Protects against: TaskStatus.valueOf throwing on an unknown status, which
    // surfaced as a 500 instead of a 400.
    @Test
    void invalidStatusReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "BOGUS"))
                .andExpect(status().isBadRequest());
    }

    // Protects against: page below 1 causing a negative subList index (500).
    // The controller intentionally rejects it with 400 rather than clamping.
    @Test
    void pageBelowOneReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "0"))
                .andExpect(status().isBadRequest());
    }

    // Protects against: (page - 1) * pageSize overflowing int for a very large
    // page, producing a negative start and an IndexOutOfBoundsException (500).
    @Test
    void pageOverflowDoesNotReturn500() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "2147483647"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", is(org.hamcrest.Matchers.empty())));
    }
}
