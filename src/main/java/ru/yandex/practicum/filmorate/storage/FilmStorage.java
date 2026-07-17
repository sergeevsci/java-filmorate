package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.Optional;

public interface FilmStorage {
    Film save(Film film);

    Film update(Film film);

    boolean exists(Long id);

    Collection<Film> findAll();

    Collection<Film> findPopular(int limit);

    Optional<Film> findById(Long id);

    void addLike(Long filmId, Long userId);

    void deleteLike(Long filmId, Long userId);
}
