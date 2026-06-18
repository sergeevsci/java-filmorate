package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserStorage userStorage;

    @InjectMocks
    private UserService userService;

    @Test
    void createReplacesBlankNameWithLogin() {
        User user = validUser(0L);
        user.setId(null);
        user.setName(" ");
        when(userStorage.findAll()).thenReturn(List.of());
        when(userStorage.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.create(user);

        assertEquals("login0", created.getName());
        verify(userStorage).save(user);
    }

    @Test
    void createRejectsDuplicateLoginOrEmail() {
        User existing = validUser(1L);
        User duplicate = validUser(0L);
        duplicate.setId(null);
        duplicate.setLogin(existing.getLogin().toUpperCase());
        when(userStorage.findAll()).thenReturn(List.of(existing));

        assertThrows(DuplicatedDataException.class, () -> userService.create(duplicate));
        verify(userStorage, never()).save(any());
    }

    @Test
    void createRejectsLoginWithSpace() {
        User user = validUser(0L);
        user.setId(null);
        user.setLogin("bad login");

        assertThrows(ConditionsNotMetException.class, () -> userService.create(user));
        verify(userStorage, never()).save(any());
    }

    @Test
    void updateRejectsUnknownUser() {
        User user = validUser(99L);
        when(userStorage.exists(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> userService.update(user));
        verify(userStorage, never()).update(any());
    }

    @Test
    void addFriendUpdatesBothUsers() {
        User user = validUser(1L);
        User friend = validUser(2L);
        when(userStorage.findById(1L)).thenReturn(Optional.of(user));
        when(userStorage.findById(2L)).thenReturn(Optional.of(friend));

        userService.addFriend(1L, 2L);

        assertEquals(List.of(2L), List.copyOf(user.getFriends()));
        assertEquals(List.of(1L), List.copyOf(friend.getFriends()));
        verify(userStorage).update(user);
        verify(userStorage).update(friend);
    }

    @Test
    void addFriendRejectsSelfFriendship() {
        assertThrows(ConditionsNotMetException.class, () -> userService.addFriend(1L, 1L));
        verify(userStorage, never()).findById(any());
    }

    @Test
    void deleteFriendRemovesFriendshipFromBothUsers() {
        User user = validUser(1L);
        User friend = validUser(2L);
        user.getFriends().add(2L);
        friend.getFriends().add(1L);
        when(userStorage.findById(1L)).thenReturn(Optional.of(user));
        when(userStorage.findById(2L)).thenReturn(Optional.of(friend));

        userService.deleteFriend(1L, 2L);

        assertEquals(List.of(), List.copyOf(user.getFriends()));
        assertEquals(List.of(), List.copyOf(friend.getFriends()));
        verify(userStorage).update(user);
        verify(userStorage).update(friend);
    }

    @Test
    void getFriendsReturnsFriendUsers() {
        User user = validUser(1L);
        User friend = validUser(2L);
        user.getFriends().add(2L);
        when(userStorage.findById(1L)).thenReturn(Optional.of(user));
        when(userStorage.findById(2L)).thenReturn(Optional.of(friend));

        Collection<User> friends = userService.getFriends(1L);

        assertEquals(List.of(friend), friends);
    }

    @Test
    void getCommonFriendsReturnsIntersection() {
        User user = validUser(1L);
        User other = validUser(2L);
        User common = validUser(3L);
        user.getFriends().add(3L);
        user.getFriends().add(4L);
        other.getFriends().add(3L);
        when(userStorage.findById(1L)).thenReturn(Optional.of(user));
        when(userStorage.findById(2L)).thenReturn(Optional.of(other));
        when(userStorage.findById(3L)).thenReturn(Optional.of(common));

        Collection<User> commonFriends = userService.getCommonFriends(1L, 2L);

        assertEquals(List.of(common), commonFriends);
    }

    @Test
    void getUserOrThrowRejectsUnknownUser() {
        when(userStorage.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getUserOrThrow(99L));
    }

    private User validUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("mail" + id + "@example.com");
        user.setLogin("login" + id);
        user.setName("name" + id);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
