package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final InMemoryUserStorage userStorage;

    // хранилище пользователей через конструктор
    public UserController(InMemoryUserStorage userStorage) {
        this.userStorage = userStorage;
    }

    @GetMapping
    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        validate(user);
        User savedUser = userStorage.save(user);
        log.info("Успешно зарегистрирован пользователь: '{}' (ID: {})", savedUser.getLogin(), savedUser.getId());
        return savedUser;
    }

    @PutMapping
    public User update(@RequestBody User newUser) {
        validate(newUser);

        if (newUser.getId() == null) {
            log.warn("Ошибка обновления пользователя: не указан ID");
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        if (!userStorage.exists(newUser.getId())) {
            log.warn("Ошибка обновления пользователя: пользователь с ID {} не найден", newUser.getId());
            throw new NotFoundException("Пользователь не найден");
        }

        User updatedUser = userStorage.update(newUser);
        log.info("Успешно обновлен пользователь с ID: {}", updatedUser.getId());
        return updatedUser;
    }

    private void validate(User user) {
        // Проверка на дубликаты
        if (user.getId() == null) {
            boolean isDuplicate = userStorage.findAll().stream()
                    .anyMatch(u -> u.getLogin().equalsIgnoreCase(user.getLogin())
                            || u.getEmail().equalsIgnoreCase(user.getEmail()));

            if (isDuplicate) {
                log.warn("Валидация пользователя провалена: логин '{}' или email '{}' уже заняты",
                        user.getLogin(), user.getEmail());
                throw new DuplicatedDataException("Пользователь с таким логином или электронной почтой уже существует");
            }
        }

        // Электронная почта не может быть пустой и должна содержать символ @
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Валидация пользователя провалена: пустой email");
            throw new ConditionsNotMetException("Электронная почта не может быть пустой");
        }
        if (!user.getEmail().contains("@")) {
            log.warn("Валидация пользователя провалена: неверный формат email '{}'", user.getEmail());
            throw new ConditionsNotMetException("Электронная почта должна содержать символ @");
        }

        // Логин не может быть пустым и содержать пробелы
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            log.warn("Валидация пользователя провалена: пустой логин");
            throw new ConditionsNotMetException("Логин не может быть пустым");
        }
        if (user.getLogin().contains(" ")) {
            log.warn("Валидация пользователя провалена: логин '{}' содержит пробелы", user.getLogin());
            throw new ConditionsNotMetException("Логин не может содержать пробелы");
        }

        // Дата рождения не может быть в будущем
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Валидация пользователя '{}' провалена: дата рождения {} в будущем",
                    user.getLogin(), user.getBirthday());
            throw new ConditionsNotMetException("Дата рождения не может быть в будущем");
        }

        // Имя для отображения может быть пустым — в таком случае будет использован логин
        if (user.getName() == null || user.getName().isBlank()) {
            log.info("Пользователь '{}' не указал имя, используем логин в качестве имени", user.getLogin());
            user.setName(user.getLogin());
        }
    }
}

