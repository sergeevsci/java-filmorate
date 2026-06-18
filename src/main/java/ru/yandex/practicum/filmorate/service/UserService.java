package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    public User create(User user) {
        validate(user);
        User savedUser = userStorage.save(user);
        log.info("Успешно зарегистрирован пользователь: '{}' (ID: {})", savedUser.getLogin(), savedUser.getId());
        return savedUser;
    }

    public User update(User newUser) {
        validate(newUser);

        if (!userStorage.exists(newUser.getId())) {
            log.warn("Ошибка обновления пользователя: пользователь с ID {} не найден", newUser.getId());
            throw new NotFoundException("Пользователь не найден");
        }

        User updatedUser = userStorage.update(newUser);
        log.info("Успешно обновлен пользователь с ID: {}", updatedUser.getId());
        return updatedUser;
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Попытка добавить себя в друзья: ID {}", userId);
            throw new ConditionsNotMetException("Пользователь не может добавить сам себя в друзья");
        }

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        user.getFriends().add(friendId); // проверок одобрения нет
        friend.getFriends().add(userId); // Взаимное добавление (друг у друга в друзьях)

        userStorage.update(user);
        userStorage.update(friend);

        log.info("Пользователи ID {} и ID {} теперь друзья", userId, friendId);
    }

    public void deleteFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Попытка удалить самого себя из друзей: ID {}", userId);
            throw new ConditionsNotMetException("Пользователь не может удалить сам себя из друзей");
        }

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        boolean removedFromUser = user.getFriends().remove(friendId);
        boolean removedFromFriend = friend.getFriends().remove(userId);

        userStorage.update(user);
        userStorage.update(friend);

        log.info("Пользователи ID {} и ID {} больше не друзья", userId, friendId);
    }

    public Collection<User> getFriends(Long userId) {
        User user = getUserOrThrow(userId);
        return user.getFriends().stream()
                .map(this::getUserOrThrow)
                .toList();
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        User user = getUserOrThrow(userId);
        User otherUser = getUserOrThrow(otherId);

        Set<Long> commonIds = new HashSet<>(user.getFriends());

        commonIds.retainAll(otherUser.getFriends());

        return commonIds.stream()
                .map(this::getUserOrThrow)
                .toList();
    }

    public User getUserOrThrow(Long id) {
        return userStorage.findById(id)  // Быстрый поиск O(1)
                .orElseThrow(() -> {
                    log.warn("Пользователь с ID {} не найден", id);
                    return new NotFoundException("Пользователь с ID " + id + " не найден");
                });
    }

    private void validate(User user) {
        if (user.getLogin() != null && user.getLogin().contains(" ")) {
            log.warn("Валидация пользователя провалена: логин содержит пробелы");
            throw new ConditionsNotMetException("Логин не может содержать пробелы");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        if (user.getId() == null) {
            boolean isDuplicate = userStorage.findAll().stream()
                    .anyMatch(u -> (u.getLogin() != null && user.getLogin() != null && u.getLogin().equalsIgnoreCase(user.getLogin()))
                            || (u.getEmail() != null && user.getEmail() != null && u.getEmail().equalsIgnoreCase(user.getEmail())));

            if (isDuplicate) {
                log.warn("Валидация пользователя провалена: логин '{}' или email '{}' уже заняты", user.getLogin(), user.getEmail());
                throw new DuplicatedDataException("Пользователь с таким логином или email уже существует");
            }
        }
    }
}

