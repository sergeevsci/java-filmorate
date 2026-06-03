package ru.yandex.practicum.filmorate.controller;

import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.validation.OnCreate;
import ru.yandex.practicum.filmorate.validation.OnUpdate;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserStorage userStorage;

    @PostMapping
    public User create(@Validated(OnCreate.class) @RequestBody User user) { // Добавили @Valid
        validate(user);
        User savedUser = userStorage.save(user);
        log.info("Успешно зарегистрирован пользователь: '{}' (ID: {})", savedUser.getLogin(), savedUser.getId());
        return savedUser;
    }

    @PutMapping
    public User update(@Validated(OnUpdate.class) @RequestBody User newUser) { // Добавили @Valid
        validate(newUser);

        if (!userStorage.exists(newUser.getId())) {
            log.warn("Ошибка обновления пользователя: пользователь с ID {} не найден", newUser.getId());
            throw new NotFoundException("Пользователь не найден");
        }

        User updatedUser = userStorage.update(newUser);
        log.info("Успешно обновлен пользователь с ID: {}", updatedUser.getId());
        return updatedUser;
    }

    @GetMapping
    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    private void validate(User user) {
        // Проверки на пустой email, @, пустой логин и дату рождения в будущем теперь делает Spring

        // осталось проверить только пробелы в логине
        // Проверка на null перед проверкой пробелов в логине для тестов Мок
        if (user.getLogin() != null && user.getLogin().contains(" ")) {
            log.warn("Валидация пользователя провалена: логин содержит пробелы");
            throw new ConditionsNotMetException("Логин не может содержать пробелы");
        }

        // Подмена пустого имени логином (тут null-safe уже встроен)
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        // Проверка на дубликаты
        if (user.getId() == null) {
            boolean isDuplicate = userStorage.findAll().stream()
                    .anyMatch(u -> u.getLogin() != null && user.getLogin() != null
                            && u.getLogin().equalsIgnoreCase(user.getLogin())
                            || u.getEmail() != null && user.getEmail() != null
                            && u.getEmail().equalsIgnoreCase(user.getEmail()));
            if (isDuplicate) {
                log.warn("Валидация пользователя провалена: логин '{}' или email '{}' уже заняты", user.getLogin(), user.getEmail());
                throw new DuplicatedDataException("Пользователь с таким логином или email уже существует");
            }
        }
    }
}
