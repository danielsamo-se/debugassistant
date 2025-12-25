package com.debugassistant.backend.controller;

import com.debugassistant.backend.dto.history.HistoryResponse;
import com.debugassistant.backend.entity.SearchHistory;
import com.debugassistant.backend.entity.User;
import com.debugassistant.backend.service.HistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HistoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private HistoryService historyService;

    private UsernamePasswordAuthenticationToken auth(User user) {
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    void shouldSaveHistoryWhenAuthenticated() throws Exception {
        User user = mock(User.class);

        SearchHistory saved = mock(SearchHistory.class);
        HistoryResponse responseDto = mock(HistoryResponse.class);

        when(historyService.saveSearch(eq(user), anyString(), any(), anyString(), anyString()))
                .thenReturn(saved);

        try (MockedStatic<HistoryResponse> mocked = Mockito.mockStatic(HistoryResponse.class)) {
            mocked.when(() -> HistoryResponse.from(saved)).thenReturn(responseDto);

            mockMvc.perform(post("/api/history")
                            .with(authentication(auth(user)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "stackTraceSnippet": "NullPointerException at X",
                                      "language": "JAVA",
                                      "exceptionType": "NullPointerException",
                                      "searchUrl": "https://example.com"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            verify(historyService, times(1)).saveSearch(eq(user), anyString(), any(), anyString(), anyString());
            verifyNoMoreInteractions(historyService);
        }
    }

    @Test
    void shouldGetHistoryWhenAuthenticated() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        SearchHistory h1 = mock(SearchHistory.class);
        SearchHistory h2 = mock(SearchHistory.class);

        when(historyService.getUserHistory(1L)).thenReturn(List.of(h1, h2));

        try (MockedStatic<HistoryResponse> mocked = Mockito.mockStatic(HistoryResponse.class)) {
            mocked.when(() -> HistoryResponse.from(h1)).thenReturn(mock(HistoryResponse.class));
            mocked.when(() -> HistoryResponse.from(h2)).thenReturn(mock(HistoryResponse.class));

            mockMvc.perform(get("/api/history")
                            .with(authentication(auth(user))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));

            verify(historyService, times(1)).getUserHistory(1L);
            verifyNoMoreInteractions(historyService);
        }
    }

    @Test
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/history"))
                .andExpect(status().is4xxClientError()); // meist 401 oder 403 je nach EntryPoint
        verifyNoInteractions(historyService);
    }
}