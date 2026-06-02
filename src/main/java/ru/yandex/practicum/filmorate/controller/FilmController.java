package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.*;

import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;

import java.time.LocalDate;
import java.util.Collection;


@RestController
@RequestMapping("/films")
public class FilmController {

    // хранилище через конструктор
    private final InMemoryFilmStorage filmStorage;

    public FilmController(InMemoryFilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    @GetMapping
    public Collection<Film>  findAll() {
        return filmStorage.findAll();
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        validate(film);
        return filmStorage.save(film);
    }

    @PutMapping
    public Film update(@RequestBody Film newFilm) {
        validate(newFilm);

        if (newFilm.getId() == null) {
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        if (!filmStorage.exists(newFilm.getId())) {
            throw new NotFoundException("Фильм не найден");
        }

        return filmStorage.update(newFilm);
    }

    private void validate(Film film) {
        // Проверка на дубликаты (название + год релиза) при создании фильма, так как могут быть фильмы с одинаковым названием
        if (film.getId() == null && film.getReleaseDate() != null) {
            boolean isDuplicate = filmStorage.findAll().stream()
                    .anyMatch(f -> f.getName().equalsIgnoreCase(film.getName())
                            && f.getReleaseDate().getYear() == film.getReleaseDate().getYear());

            if (isDuplicate) {
                throw new DuplicatedDataException("Фильм с названием '" + film.getName() + "' за "
                        + film.getReleaseDate().getYear() + " год уже существует");
            }
        }

        if (film.getName() == null || film.getName().isBlank()) {
            throw new ConditionsNotMetException("Название фильма не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ConditionsNotMetException("Максимальная длина описания — 200 символов");
        }

        if (film.getReleaseDate() != null) {
            LocalDate cinemaBirthDate = LocalDate.of(1895, 12, 28);
            if (film.getReleaseDate().isBefore(cinemaBirthDate)) {
                throw new ConditionsNotMetException("Дата релиза должна быть не раньше 28 декабря 1895 года");
            }
        }

        if (film.getDuration() == null || film.getDuration().isNegative() || film.getDuration().isZero()) {
            throw new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом");
        }
    }
}
