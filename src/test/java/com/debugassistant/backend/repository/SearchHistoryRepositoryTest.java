package com.debugassistant.backend.repository;

import com.debugassistant.backend.entity.SearchHistory;
import com.debugassistant.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class SearchHistoryRepositoryTest {

    @Autowired
    private SearchHistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindHistoryByUserOrderedByDateDesc() {
        // Arrange
        User user = User.builder().email("a@b.c").passwordHash("pw").build();
        userRepository.save(user);

        SearchHistory oldSearch = SearchHistory.builder()
                .user(user)
                .exceptionType("OldException")
                .searchedAt(LocalDateTime.now().minusHours(2))
                .build();

        SearchHistory newSearch = SearchHistory.builder()
                .user(user)
                .exceptionType("NewException")
                .searchedAt(LocalDateTime.now())
                .build();

        historyRepository.save(oldSearch);
        historyRepository.save(newSearch);

        // Act
        List<SearchHistory> results = historyRepository.findByUserIdOrderBySearchedAtDesc(user.getId());

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getExceptionType()).isEqualTo("NewException"); // Neueste zuerst
        assertThat(results.get(1).getExceptionType()).isEqualTo("OldException");
    }
}