package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.yandex.practicum.filmorate.validation.OnCreate;
import ru.yandex.practicum.filmorate.validation.OnUpdate;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(of = "id")
public class User {
    @Null(groups = OnCreate.class, message = "ID должен быть null при создании")
    @NotNull(groups = OnUpdate.class, message = "ID обязателен для обновления")
    private Long id;

    @Email(groups = {OnCreate.class, OnUpdate.class}, message = "Некорректный формат почты")
    @NotBlank(groups = OnCreate.class, message = "Почта не может быть пустой")
    private String email;

    @NotBlank(groups = OnCreate.class, message = "Логин не может быть пустым")
    private String login;

    private String name;

    @PastOrPresent(groups = {OnCreate.class, OnUpdate.class}, message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;
}

