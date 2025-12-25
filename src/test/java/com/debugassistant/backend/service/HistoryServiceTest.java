package com.debugassistant.backend.service;

import com.debugassistant.backend.entity.SearchHistory;
import com.debugassistant.backend.entity.User;
import com.debugassistant.backend.repository.SearchHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private SearchHistoryRepository historyRepository;

    @InjectMocks
    private HistoryService historyService;

    private static final String COMPLEX_JAVA_TRACE = """
            org.springframework.context.ApplicationContextException: Unable to start web server; nested exception is org.springframework.boot.web.server.WebServerException: Unable to start embedded Tomcat
                at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.onRefresh(ServletWebServerApplicationContext.java:156)
                at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:544)
            Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'entityManagerFactory' defined in class path resource [org/springframework/boot/autoconfigure/orm/jpa/HibernateJpaConfiguration.class]: Invocation of init method failed; nested exception is javax.persistence.PersistenceException: [PersistenceUnit: default] Unable to build Hibernate SessionFactory; nested exception is org.hibernate.exception.GenericJDBCException: Unable to open JDBC Connection for DDL execution
                at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.initializeBean(AbstractAutowireCapableBeanFactory.java:1796)
            Caused by: org.postgresql.util.PSQLException: FATAL: password authentication failed for user "postgres"
                at org.postgresql.core.v3.ConnectionFactoryImpl.doAuthentication(ConnectionFactoryImpl.java:514)
                at org.postgresql.core.v3.ConnectionFactoryImpl.tryConnect(ConnectionFactoryImpl.java:141)
            """;

    private static final String SIMPLE_PYTHON_TRACE = "Traceback (most recent call last):\n  File \"app.py\", line 10, in <module>\nZeroDivisionError: division by zero";

    @Test
    void shouldTruncateComplexSpringTrace() {
        User user = new User();
        user.setId(1L);

        when(historyRepository.save(any(SearchHistory.class))).thenAnswer(i -> i.getArgument(0));

        SearchHistory result = historyService.saveSearch(
                user,
                COMPLEX_JAVA_TRACE,
                "Java",
                "PSQLException",
                "https://stackoverflow.com/questions/123"
        );

        assertNotNull(result);
        assertEquals(500, result.getStackTraceSnippet().length(), "Snippet must be exactly 500 chars long");
        assertTrue(result.getStackTraceSnippet().startsWith("org.springframework.context.ApplicationContextException"), "Start of trace must be preserved");

        verify(historyRepository).save(any(SearchHistory.class));
    }

    @Test
    void shouldSaveShortTraceWithoutModification() {
        User user = new User();

        when(historyRepository.save(any(SearchHistory.class))).thenAnswer(i -> i.getArgument(0));

        SearchHistory result = historyService.saveSearch(
                user,
                SIMPLE_PYTHON_TRACE,
                "Python",
                "ZeroDivisionError",
                "https://github.com/test"
        );

        assertEquals(SIMPLE_PYTHON_TRACE.length(), result.getStackTraceSnippet().length());
        assertEquals(SIMPLE_PYTHON_TRACE, result.getStackTraceSnippet());
        assertEquals("Python", result.getLanguage());
    }

    @Test
    void shouldHandleNullSnippet() {
        User user = new User();
        when(historyRepository.save(any(SearchHistory.class))).thenAnswer(i -> i.getArgument(0));

        SearchHistory result = historyService.saveSearch(
                user,
                null,
                "Java",
                "NullPointerException",
                "https://example.com"
        );

        assertNotNull(result);
        assertEquals("", result.getStackTraceSnippet());
    }

    @Test
    void shouldHandleNullSearchUrl() {
        User user = new User();
        when(historyRepository.save(any(SearchHistory.class))).thenAnswer(i -> i.getArgument(0));

        SearchHistory result = historyService.saveSearch(
                user,
                "Error...",
                "Go",
                "Panic",
                null
        );

        assertNotNull(result);
        assertNull(result.getSearchUrl());
        assertEquals("Panic", result.getExceptionType());
    }

    @Test
    void shouldRetrieveUserHistory() {
        Long userId = 1L;
        SearchHistory older = SearchHistory.builder().searchedAt(LocalDateTime.now().minusDays(1)).build();
        SearchHistory newer = SearchHistory.builder().searchedAt(LocalDateTime.now()).build();

        when(historyRepository.findByUserIdOrderBySearchedAtDesc(userId))
                .thenReturn(List.of(newer, older));

        List<SearchHistory> history = historyService.getUserHistory(userId);

        assertEquals(2, history.size());
        verify(historyRepository).findByUserIdOrderBySearchedAtDesc(userId);
    }
}