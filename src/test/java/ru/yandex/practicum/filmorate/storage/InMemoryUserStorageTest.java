package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUserStorageTest {
    private InMemoryUserStorage userStorage;

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage();
    }

    @Test
    void saveGeneratesIdAndFindByIdReturnsUser() {
        User savedUser = userStorage.save(makeUser("first"));

        assertThat(savedUser.getId()).isEqualTo(1L);
        assertThat(userStorage.findById(savedUser.getId()))
                .isPresent()
                .hasValueSatisfying(user -> assertThat(user.getLogin()).isEqualTo("first"));
    }

    @Test
    void updateChangesOnlyNotNullFields() {
        User savedUser = userStorage.save(makeUser("second"));
        User update = new User();
        update.setId(savedUser.getId());
        update.setEmail("updated@example.com");
        update.setName("Updated Name");

        User updatedUser = userStorage.update(update);

        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getLogin()).isEqualTo("second");
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
    }

    @Test
    void existsReturnsTrueOnlyForStoredUser() {
        User savedUser = userStorage.save(makeUser("third"));

        assertThat(userStorage.exists(savedUser.getId())).isTrue();
        assertThat(userStorage.exists(999L)).isFalse();
    }

    @Test
    void findAllReturnsStoredUsers() {
        User firstUser = userStorage.save(makeUser("firstall"));
        User secondUser = userStorage.save(makeUser("secondall"));

        assertThat(userStorage.findAll())
                .extracting(User::getId)
                .contains(firstUser.getId(), secondUser.getId());
    }

    @Test
    void addAndDeleteFriendIsOneWay() {
        User user = userStorage.save(makeUser("user"));
        User friend = userStorage.save(makeUser("friend"));

        userStorage.addFriend(user.getId(), friend.getId());

        assertThat(user.getFriends()).containsExactly(friend.getId());
        assertThat(friend.getFriends()).isEmpty();

        userStorage.deleteFriend(user.getId(), friend.getId());

        assertThat(user.getFriends()).isEmpty();
    }

    @Test
    void findByIdReturnsEmptyForUnknownUser() {
        assertThat(userStorage.findById(999L)).isEmpty();
    }

    private User makeUser(String login) {
        User user = new User();
        user.setEmail(login + "@example.com");
        user.setLogin(login);
        user.setName("Name " + login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
