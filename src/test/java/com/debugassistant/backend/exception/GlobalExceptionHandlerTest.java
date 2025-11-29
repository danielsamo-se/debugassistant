package com.debugassistant.backend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(jsonPath("$.message").value("invalid trace"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn422_ForUnsupportedLanguage() throws Exception {
        mockMvc.perform(get("/test/lang"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("lang not supported"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400_ForValidationError() throws Exception {
        mockMvc.perform(get("/test/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name must not be empty"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ----------------------------
    //        TEST CONTROLLER
    // ----------------------------

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/invalid")
        public void invalid() {
            throw new InvalidStackTraceException("invalid trace");
        }

        @GetMapping("/lang")
        public void lang() {
            throw new UnsupportedLanguageException("lang not supported");
        }

        @GetMapping("/validate")
        public void validate() throws Exception {

            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "obj");

            bindingResult.addError(
                    new FieldError("obj", "name", "name must not be empty")
            );

            Method method = TestController.class.getMethod("validate");
            MethodParameter param = new MethodParameter(method, -1);

            throw new MethodArgumentNotValidException(param, bindingResult);
        }
    }
}