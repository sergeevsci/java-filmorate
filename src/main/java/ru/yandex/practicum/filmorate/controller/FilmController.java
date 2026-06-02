package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;

import java.time.LocalDate;
import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {

    private final InMemoryFilmStorage filmStorage;

    public FilmController(InMemoryFilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) { // Добавили @Valid
        validate(film);
        Film savedFilm = filmStorage.save(film);
        log.info("Успешно добавлен новый фильм: '{}' (ID: {})", savedFilm.getName(), savedFilm.getId());
        return savedFilm;
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film newFilm) { // Добавили @Valid
        validate(newFilm);

        if (newFilm.getId() == null) {
            log.warn("Ошибка обновления фильма: не указан ID");
            throw new ConditionsNotMetException("Id должен быть указан");
        }
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

        // Кастомная проверка даты релиза - ее не сделать аннотациями
        LocalDate cinemaBirthDate = LocalDate.of(1895, 12, 28);
        // Проверка на null перед вызовом методов даты
        if (film.getReleaseDate() != null) { // для тестов Мок, а то падаем с Null
            if (film.getReleaseDate().isBefore(cinemaBirthDate)) {
                log.warn("Валидация фильма провалена: дата релиза раньше 28.12.1895");
                throw new ConditionsNotMetException("Дата релиза должна быть не раньше 28 декабря 1895 года");
            }
        }

        // Проверка на null перед вызовом методов duration
        if (film.getDuration() != null) {
            if (film.getDuration().isNegative() || film.getDuration().isZero()) {
                log.warn("Валидация фильма провалена: некорректная продолжительность");
                throw new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом");
            }
        }

        // Проверка на дубликаты
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
