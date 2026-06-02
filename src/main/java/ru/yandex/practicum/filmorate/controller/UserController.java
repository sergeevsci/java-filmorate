package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.*;

import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.Collection;

@RestController
@RequestMapping("/users")
public class UserController {

    private final InMemoryUserStorage userStorage;

    // хранилище пользователей через конструктор
    public UserController(InMemoryUserStorage userStorage) {
        this.userStorage = userStorage;
    }

    @PostMapping
    public User create(@RequestBody User user) {
        validate(user);
        return userStorage.save(user);
    }

    @PutMapping
    public User update(@RequestBody User newUser) {
        validate(newUser);

        if (newUser.getId() == null) {
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        if (!userStorage.exists(newUser.getId())) {
            throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
        }

        return userStorage.update(newUser);
    }

    @GetMapping
    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    private void validate(User user) {
        // Проверка на дубликаты
        if (user.getId() == null) {
            boolean isDuplicate = userStorage.findAll().stream()
                    .anyMatch(u -> u.getLogin().equalsIgnoreCase(user.getLogin())
                            || u.getEmail().equalsIgnoreCase(user.getEmail()));

            if (isDuplicate) {
                throw new DuplicatedDataException("Пользователь с таким логином или электронной почтой уже существует");
            }
        }

        // Электронная почта не может быть пустой и должна содержать символ @
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ConditionsNotMetException("Электронная почта не может быть пустой");
        }
        if (!user.getEmail().contains("@")) {
            throw new ConditionsNotMetException("Электронная почта должна содержать символ @");
        }

        // Логин не может быть пустым и содержать пробелы
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ConditionsNotMetException("Логин не может быть пустым");
        }
        if (user.getLogin().contains(" ")) {
            throw new ConditionsNotMetException("Логин не может содержать пробелы");
        }

        // Дата рождения не может быть в будущем
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ConditionsNotMetException("Дата рождения не может быть в будущем");
        }

        // Имя для отображения может быть пустым — в таком случае будет использован логин
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}

