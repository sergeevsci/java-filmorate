package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.yandex.practicum.filmorate.validation.AfterCinemaBirthDate;
import ru.yandex.practicum.filmorate.validation.OnCreate;
import ru.yandex.practicum.filmorate.validation.OnUpdate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(of = "id")
public class Film {
    @Null(groups = OnCreate.class, message = "ID должен быть null при создании")
    @NotNull(groups = OnUpdate.class, message = "ID обязателен для обновления")
    private Long id;

    @NotBlank(groups = OnCreate.class, message = "Название не может быть пустым")
    private String name;

    @Size(max = 200, groups = {OnCreate.class, OnUpdate.class}, message = "Максимальная длина описания — 200 символов")
    private String description;

    @AfterCinemaBirthDate(groups = {OnCreate.class, OnUpdate.class})
    @NotNull(groups = OnCreate.class, message = "Дата релиза должна быть указана")
    private LocalDate releaseDate;

    @Positive(groups = {OnCreate.class, OnUpdate.class}, message = "Продолжительность фильма должна быть больше нуля")
    private Integer duration; // обертка Интеджер - для памяти. там надо дать возможность на null

    private Set<Long> likes = new HashSet<>();

    // Жанры (Set из рекордов Genre)
    private Set<@NotNull(groups = {OnCreate.class, OnUpdate.class}, message = "Жанр не может быть null") Genre> genres = new LinkedHashSet<>();

    // Рейтинг MPA (одиночный рекорд Mpa)
    @NotNull(groups = OnCreate.class, message = "Рейтинг MPA должен быть указан")
    private Mpa mpa;
}

