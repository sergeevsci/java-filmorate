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

    public void addLike(Long filmId, Long userId) {
        // Проверяем, что пользователь существует
        userService.getFriends(userId); // Если пользователя нет, метод сбросит NotFoundException

        Film film = getFilmOrThrow(filmId);

        film.getLikes().add(userId);
        filmStorage.update(film); // Сохраняем изменения в хранилище

        log.info("Пользователь с ID {} поставил лайк фильму с ID {}", userId, filmId);
    }

    public void deleteLike(Long filmId, Long userId) {
        userService.getUserOrThrow(userId);

        Film film = getFilmOrThrow(filmId);

        if (!film.getLikes().contains(userId)) {
            log.warn("Пользователь ID {} не ставил лайк фильму ID {}", userId, filmId);
            throw new NotFoundException("Лайк от пользователя с ID " + userId + " не найден");
        }

        film.getLikes().remove(userId);
        filmStorage.update(film);

        log.info("Пользователь с ID {} удалил лайк с фильма с ID {}", userId, filmId);
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
        return filmStorage.findById(id) // быстрый поиск тк у нас хэшмапа
                .orElseThrow(() -> {
                    log.warn("Фильм с ID {} не найден", id);
                    return new NotFoundException("Фильм с ID " + id + " не найден");
                });
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
