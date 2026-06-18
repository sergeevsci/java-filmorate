package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmServiceTest {

    @Mock
    private FilmStorage filmStorage;

    @Mock
    private UserService userService;

    @InjectMocks
    private FilmService filmService;

    @Test
    void createRejectsDuplicateFilmForSameYear() {
        Film existing = validFilm(1L);
        Film duplicate = validFilm(null);
        duplicate.setName(existing.getName().toUpperCase());
        when(filmStorage.findAll()).thenReturn(List.of(existing));

        assertThrows(DuplicatedDataException.class, () -> filmService.create(duplicate));
        verify(filmStorage, never()).save(any());
    }

    @Test
    void createSavesNewFilm() {
        Film film = validFilm(null);
        when(filmStorage.findAll()).thenReturn(List.of());
        when(filmStorage.save(film)).thenReturn(film);

        Film created = filmService.create(film);

        assertEquals(film, created);
        verify(filmStorage).save(film);
    }

    @Test
    void updateRejectsUnknownFilm() {
        Film film = validFilm(99L);
        when(filmStorage.exists(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> filmService.update(film));
        verify(filmStorage, never()).update(any());
    }

    @Test
    void addLikeChecksUserAndUpdatesFilm() {
        Film film = validFilm(1L);
        when(filmStorage.findById(1L)).thenReturn(Optional.of(film));

        filmService.addLike(1L, 2L);

        assertEquals(List.of(2L), List.copyOf(film.getLikes()));
        verify(userService).getFriends(2L);
        verify(filmStorage).update(film);
    }

    @Test
    void addLikeRejectsUnknownFilm() {
        when(filmStorage.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> filmService.addLike(99L, 1L));
        verify(userService).getFriends(1L);
        verify(filmStorage, never()).update(any());
    }

    @Test
    void deleteLikeRemovesExistingLike() {
        Film film = validFilm(1L);
        film.getLikes().add(2L);
        when(filmStorage.findById(1L)).thenReturn(Optional.of(film));

        filmService.deleteLike(1L, 2L);

        assertEquals(List.of(), List.copyOf(film.getLikes()));
        verify(userService).getUserOrThrow(2L);
        verify(filmStorage).update(film);
    }

    @Test
    void deleteLikeDoesNotUpdateWhenLikeDoesNotExist() {
        Film film = validFilm(1L);
        when(filmStorage.findById(1L)).thenReturn(Optional.of(film));

        filmService.deleteLike(1L, 2L);

        verify(userService).getUserOrThrow(2L);
        verify(filmStorage, never()).update(any());
    }

    @Test
    void getPopularFilmsSortsByLikesAndLimitsCount() {
        Film first = validFilm(1L);
        Film second = validFilm(2L);
        Film third = validFilm(3L);
        first.getLikes().add(1L);
        second.getLikes().add(1L);
        second.getLikes().add(2L);
        when(filmStorage.findAll()).thenReturn(List.of(first, second, third));

        List<Film> popular = List.copyOf(filmService.getPopularFilms(2));

        assertEquals(List.of(second, first), popular);
    }

    @Test
    void getPopularFilmsUsesDefaultLimitForNonPositiveCount() {
        Film first = validFilm(1L);
        when(filmStorage.findAll()).thenReturn(List.of(first));

        List<Film> popular = List.copyOf(filmService.getPopularFilms(0));

        assertEquals(List.of(first), popular);
    }

    private Film validFilm(Long id) {
        Film film = new Film();
        film.setId(id);
        film.setName("Film" + id);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(90);
        return film;
    }
}
