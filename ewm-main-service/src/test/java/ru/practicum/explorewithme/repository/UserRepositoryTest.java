package ru.practicum.explorewithme.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.explorewithme.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveUser() {
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .build();

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("Test User", saved.getName());
        assertEquals("test@example.com", saved.getEmail());
    }

    @Test
    void findByIdIn() {
        User user1 = User.builder().name("User1").email("user1@example.com").build();
        User user2 = User.builder().name("User2").email("user2@example.com").build();

        userRepository.save(user1);
        userRepository.save(user2);

        List<User> users = userRepository.findByIdIn(List.of(user1.getId(), user2.getId()), null);

        assertEquals(2, users.size());
    }
}