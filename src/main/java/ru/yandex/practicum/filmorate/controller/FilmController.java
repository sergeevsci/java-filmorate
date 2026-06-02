package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;

import java.time.LocalDate;
import java.util.Collection;

@Slf4j // Lombok автоматически создает поле private static final Logger log
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
        Film savedFilm = filmStorage.save(film);
        log.info("Успешно добавлен новый фильм: '{}' (ID: {})", savedFilm.getName(), savedFilm.getId());
        return savedFilm;
    }

    @PutMapping
    public Film update(@RequestBody Film newFilm) {
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

    private void validate(Film film) {
        // Проверка на дубликаты (название + год релиза) при создании фильма, так как могут быть фильмы с одинаковым названием
        if (film.getId() == null && film.getReleaseDate() != null) {
            boolean isDuplicate = filmStorage.findAll().stream()
                    .anyMatch(f -> f.getName().equalsIgnoreCase(film.getName())
                            && f.getReleaseDate().getYear() == film.getReleaseDate().getYear());

            if (isDuplicate) {
                log.warn("Валидация фильма провалена: обнаружен дубликат '{}' за {} год",
                        film.getName(), film.getReleaseDate().getYear());
                throw new DuplicatedDataException("Фильм с названием '" + film.getName() + "' за "
                        + film.getReleaseDate().getYear() + " год уже существует");
            }
        }

        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Валидация фильма провалена: пустое название");
            throw new ConditionsNotMetException("Название фильма не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Валидация фильма '{}' провалена: длина описания {} символов (макс. 200)",
                    film.getName(), film.getDescription().length());
            throw new ConditionsNotMetException("Максимальная длина описания — 200 символов");
        }

        if (film.getReleaseDate() != null) {
            LocalDate cinemaBirthDate = LocalDate.of(1895, 12, 28);
            if (film.getReleaseDate().isBefore(cinemaBirthDate)) {
                log.warn("Валидация фильма '{}' провалена: дата релиза {} раньше 28.12.1895",
                        film.getName(), film.getReleaseDate());
                throw new ConditionsNotMetException("Дата релиза должна быть не раньше 28 декабря 1895 года");
            }
        }

        if (film.getDuration() == null || film.getDuration().isNegative() || film.getDuration().isZero()) {
            log.warn("Валидация фильма '{}' провалена: некорректная продолжительность", film.getName());
            throw new ConditionsNotMetException("Продолжительность фильма должна быть положительным числом");
        }
    }
}
