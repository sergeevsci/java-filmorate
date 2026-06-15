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

    public void removeFriend(Long userId, Long friendId) {
        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userStorage.update(user);
        userStorage.update(friend);

        log.info("Пользователи ID {} и ID {} удалены из друзей", userId, friendId);
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

        return user.getFriends().stream()
                .filter(otherUser.getFriends()::contains)
                .map(this::getUserOrThrow)
                .toList();
    }

    private User getUserOrThrow(Long id) { // поиск пользователя по id. если нет - exception
        return userStorage.findAll().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
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

