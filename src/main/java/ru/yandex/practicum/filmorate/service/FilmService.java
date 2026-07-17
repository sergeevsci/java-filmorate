package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.MpaDbStorage;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage, UserService userService,
                       GenreDbStorage genreStorage, MpaDbStorage mpaStorage) {
        this.filmStorage = filmStorage;
        this.userService = userService;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

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

    public Film findById(Long id) {
        return getFilmOrThrow(id);
    }

    public void addLike(Long filmId, Long userId) {
        // Проверяем, что пользователь существует
        userService.getUserOrThrow(userId); // Если пользователя нет, метод сбросит NotFoundException

        Film film = getFilmOrThrow(filmId);

        film.getLikes().add(userId);
        filmStorage.addLike(filmId, userId);

        log.info("Пользователь с ID {} поставил лайк фильму с ID {}", userId, filmId);
    }

    public void deleteLike(Long filmId, Long userId) {
        userService.getUserOrThrow(userId);
        Film film = getFilmOrThrow(filmId);

        boolean removed = film.getLikes().remove(userId);

        if (removed) {
            filmStorage.deleteLike(filmId, userId);
            log.info("Пользователь с ID {} удалил лайк с фильма с ID {}", userId, filmId);
        } else {
            // Если лайка и так не было - логируем и выходим (200 OK) - идемпотентность на Delete
            log.info("Пользователь ID {} не оставлял лайк фильму ID {}. Ничего не изменено", userId, filmId);
        }
    }

    public Collection<Film> getPopularFilms(Integer count) {
        // Если count не передан в контроллере, используем 10
        int limit = (count <= 0) ? 10 : count;

        return filmStorage.findAll().stream()
                // Сортируем по убыванию количества лайков
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(limit)
                .toList();
    }

    private Film getFilmOrThrow(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("Фильм с ID {} не найден", id);
                    return new NotFoundException("Фильм с ID " + id + " не найден");
                });
    }

    private void validate(Film film) {
        if (film.getMpa() != null && mpaStorage.findById(film.getMpa().id()).isEmpty()) {
            throw new NotFoundException("Рейтинг MPA с ID " + film.getMpa().id() + " не найден");
        }

        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        Set<Integer> genreIds = genreStorage.findAll().stream()
                .map(Genre::id)
                .collect(Collectors.toSet()); // один запрос на получение коллекции всех существующих Жанров

        for (Genre genre : film.getGenres()) { // тогда ищем уже по коллекции
            if (!genreIds.contains(genre.id())) {
                throw new NotFoundException("Жанр с ID " + genre.id() + " не найден");
            }
        }
    }
}
