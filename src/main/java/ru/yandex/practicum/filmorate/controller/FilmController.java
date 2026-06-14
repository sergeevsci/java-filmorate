package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.validation.OnCreate;
import ru.yandex.practicum.filmorate.validation.OnUpdate;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/films")
@RequiredArgsConstructor
public class FilmController {

    private final FilmStorage filmStorage; // конструктор, в котором был бы интерфейс хранилища на выбор
    // уехал в аннотацию @RequiredArgsConstructor.

    @PostMapping
    public Film create(@Validated(OnCreate.class) @RequestBody Film film) { // убрал валид, теперь валидатед
        validate(film);
        Film savedFilm = filmStorage.save(film);
        log.info("Успешно добавлен новый фильм: '{}' (ID: {})", savedFilm.getName(), savedFilm.getId());
        return savedFilm;
    }

    @PutMapping
    public Film update(@Validated(OnUpdate.class) @RequestBody Film newFilm) {
        validate(newFilm);

        if (!filmStorage.exists(newFilm.getId())) {
            log.warn("Ошибка обновления фильма: фильм с ID {} не найден", newFilm.getId());
            throw new NotFoundException("Фильм не найден");
        }

        Film updatedFilm = filmStorage.update(newFilm);
        log.info("Успешно обновлен фильм с ID: {}", updatedFilm.getId());
        return updatedFilm;
    }

    @GetMapping
    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    private void validate(Film film) {
        // Проверки на name, description и null для duration теперь под капотом Spring
        // проверка на год фильма тоже теперь у самого Film объекта. Висит аннотация

        // Проверка на дубликаты только осталась
        if (film.getId() == null && film.getReleaseDate() != null) {
            boolean isDuplicate = filmStorage.findAll().stream()
                    .anyMatch(f -> f.getName().equalsIgnoreCase(film.getName())
                            && f.getReleaseDate() != null
                            && f.getReleaseDate().getYear() == film.getReleaseDate().getYear());
            if (isDuplicate) {
                log.warn("Валидация фильма провалена: обнаружен дубликат '{}' за {} год", film.getName(), film.getReleaseDate().getYear());
                throw new DuplicatedDataException("Фильм с таким названием и годом релиза уже существует");
            }
        }
    }
}
