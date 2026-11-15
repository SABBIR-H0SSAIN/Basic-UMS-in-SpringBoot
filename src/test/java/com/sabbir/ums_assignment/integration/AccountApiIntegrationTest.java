package com.sabbir.ums_assignment.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountApiIntegrationTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class PostgresConfiguration {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:17-alpine");
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void studentCanAccessStudentsButCannotAccessAdmins() throws Exception {
        mvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());

        MockHttpSession session = login("student", "admin123", "STUDENT");

        mvc.perform(get("/api/students").session(session))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admins").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutRequiresCsrfAndInvalidatesTheAuthenticatedSession() throws Exception {
        MockHttpSession session = login("ci_admin", "ci_admin_password", "ADMIN");

        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ci_admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mvc.perform(withCsrf(post("/api/auth/logout"), session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        assertTrue(session.isInvalid(), "Logout should invalidate the authenticated session");
    }

    @Test
    void adminCanCreateReadUpdateAndDeleteAStudent() throws Exception {
        MockHttpSession session = login("ci_admin", "ci_admin_password", "ADMIN");
        String username = "sabbir-" + UUID.randomUUID();

        String location = mvc.perform(withCsrf(post("/api/students"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson(username, "Sabbir")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotHaveJsonPath())
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sabbir"))
                .andExpect(jsonPath("$.cgpa").value(3.75));

        mvc.perform(withCsrf(put(location), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson(username, "Sabbir Hossain")))
                .andExpect(status().isOk());

        mvc.perform(get(location).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sabbir Hossain"));

        mvc.perform(withCsrf(delete(location), session))
                .andExpect(status().isNoContent());

        mvc.perform(get(location).session(session))
                .andExpect(status().isNotFound());
    }

    private MockHttpSession login(String username, String password, String role) throws Exception {
        var session = new MockHttpSession();
        mvc.perform(withCsrf(post("/api/auth/login"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role));
        return session;
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request,
                                                  MockHttpSession session) throws Exception {
        String response = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String header = JsonPath.read(response, "$.headerName");
        String token = JsonPath.read(response, "$.token");
        return request.session(session).header(header, token);
    }

    private String studentJson(String username, String name) {
        return """
                {
                    "username": "%s",
                    "password": "student123",
                    "name": "%s",
                    "email": "sabbir@example.com",
                    "year": 2,
                    "semester": 1,
                    "cgpa": 3.75
                }
                """.formatted(username, name);
    }
}
