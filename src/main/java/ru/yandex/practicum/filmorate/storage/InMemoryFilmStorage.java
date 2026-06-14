package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component // бин на хранилище
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();

    @Override
    public Film save(Film film) {
        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film newFilm) {
        // Достаем существующий фильм
        Film oldFilm = films.get(newFilm.getId());

        // Обновляем в нем только те поля, которые клиент прислал в запросе (не null)
        if (newFilm.getName() != null) {
            oldFilm.setName(newFilm.getName());
        }
        if (newFilm.getDescription() != null) {
            oldFilm.setDescription(newFilm.getDescription());
        }
        if (newFilm.getReleaseDate() != null) {
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
        }
        if (newFilm.getDuration() != null) {
            oldFilm.setDuration(newFilm.getDuration());
        }

        // Сохраняем обновленный старый фильм обратно в мапу
        films.put(oldFilm.getId(), oldFilm);
        return oldFilm;
    }


    @Override
    public boolean exists(Long id) {
        return films.containsKey(id);
    }

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}

