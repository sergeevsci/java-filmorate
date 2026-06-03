package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import java.util.Collection;

public interface FilmStorage {
    Film save(Film film);
    Film update(Film film);
    boolean exists(Long id);
    Collection<Film> findAll();
}
