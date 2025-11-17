package com.debugassistant.backend.repository;

import com.debugassistant.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        // Arrange
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .passwordHash("secret_hash")
                .name("Test User")
                .build();

        // Act
        userRepository.save(user);
        Optional<User> found = userRepository.findByEmail(email);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(email);
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    void shouldCheckIfEmailExists() {
        // Arrange
        String email = "existing@example.com";
        User user = User.builder()
                .email(email)
                .passwordHash("hash")
                .build();

        userRepository.save(user);

        // Act
        boolean exists = userRepository.existsByEmail(email);
        boolean notExists = userRepository.existsByEmail("other@example.com");

        // Assert
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}