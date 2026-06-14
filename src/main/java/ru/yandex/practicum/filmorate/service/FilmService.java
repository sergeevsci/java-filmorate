package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;

    public Film create(Film film) {
        validate(film);
        Film savedFilm = filmStorage.save(film);
        log.info("Успешно добавлен новый фильм: '{}' (ID: {})", savedFilm.getName(), savedFilm.getId());
        return savedFilm;
    }

    public Film update(Film newFilm) {
        validate(newFilm);

        if (!filmStorage.exists(newFilm.getId())) {
            log.warn("Ошибка обновления фильма: фильм с ID {} не найден", newFilm.getId());
            throw new NotFoundException("Фильм не найден");
        }

        Film updatedFilm = filmStorage.update(newFilm);
        log.info("Успешно обновлен фильм с ID: {}", updatedFilm.getId());
        return updatedFilm;
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    private void validate(Film film) {
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
