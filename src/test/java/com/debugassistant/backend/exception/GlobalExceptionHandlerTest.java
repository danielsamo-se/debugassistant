package com.debugassistant.backend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn400_ForInvalidStackTrace() throws Exception {
        mockMvc.perform(get("/test/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Stack trace cannot be empty"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn422_ForUnsupportedLanguage() throws Exception {
        mockMvc.perform(get("/test/lang"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Could not detect language from stack trace"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn409_ForEmailAlreadyRegistered() throws Exception {
        mockMvc.perform(get("/test/duplicate-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn404_ForUserNotFound() throws Exception {
        mockMvc.perform(get("/test/user-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400_ForInvalidJsonPayload() throws Exception {
        mockMvc.perform(post("/test/invalid-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid JSON payload"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn401_ForBadCredentials() throws Exception {
        mockMvc.perform(get("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        record DummyDto(String a) {}

        @GetMapping("/invalid")
        public void invalid() {
            throw new InvalidStackTraceException("Stack trace cannot be empty");
        }

        @GetMapping("/lang")
        public void lang() {
            throw new UnsupportedLanguageException("Could not detect language from stack trace");
        }

        @GetMapping("/duplicate-email")
        public void duplicateEmail() {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        @GetMapping("/user-not-found")
        public void userNotFound() {
            throw new UserNotFoundException("User not found");
        }

        @PostMapping("/invalid-json")
        public void invalidJson(@RequestBody DummyDto body) {
        }

        @GetMapping("/bad-credentials")
        public void badCredentials() {
            throw new BadCredentialsException("bad");
        }
    }
}
