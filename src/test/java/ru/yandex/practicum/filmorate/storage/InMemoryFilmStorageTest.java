package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFilmStorageTest {
    private InMemoryFilmStorage filmStorage;

    @BeforeEach
    void setUp() {
        filmStorage = new InMemoryFilmStorage();
    }

    @Test
    void saveGeneratesIdAndFindByIdReturnsFilm() {
        Film savedFilm = filmStorage.save(makeFilm("First Film"));

        assertThat(savedFilm.getId()).isEqualTo(1L);
        assertThat(filmStorage.findById(savedFilm.getId()))
                .isPresent()
                .hasValueSatisfying(film -> assertThat(film.getName()).isEqualTo("First Film"));
    }

    @Test
    void updateChangesOnlyNotNullFieldsAndUpdatesMpaAndGenres() {
        Film savedFilm = filmStorage.save(makeFilm("Old Film"));
        Film update = new Film();
        update.setId(savedFilm.getId());
        update.setName("New Film");
        update.setDuration(120);
        update.setMpa(new Mpa(2, "PG"));
        update.setGenres(new LinkedHashSet<>());

        Film updatedFilm = filmStorage.update(update);

        assertThat(updatedFilm.getName()).isEqualTo("New Film");
        assertThat(updatedFilm.getDescription()).isEqualTo("Description");
        assertThat(updatedFilm.getDuration()).isEqualTo(120);
        assertThat(updatedFilm.getMpa()).isEqualTo(new Mpa(2, "PG"));
        assertThat(updatedFilm.getGenres()).isEmpty();
    }

    @Test
    void existsReturnsTrueOnlyForStoredFilm() {
        Film savedFilm = filmStorage.save(makeFilm("Existing Film"));

        assertThat(filmStorage.exists(savedFilm.getId())).isTrue();
        assertThat(filmStorage.exists(999L)).isFalse();
    }

    @Test
    void findAllReturnsStoredFilms() {
        Film firstFilm = filmStorage.save(makeFilm("First Film"));
        Film secondFilm = filmStorage.save(makeFilm("Second Film"));

        assertThat(filmStorage.findAll())
                .extracting(Film::getId)
                .contains(firstFilm.getId(), secondFilm.getId());
    }

    @Test
    void addAndDeleteLike() {
        Film film = filmStorage.save(makeFilm("Liked Film"));

        filmStorage.addLike(film.getId(), 1L);

        assertThat(film.getLikes()).containsExactly(1L);

        filmStorage.deleteLike(film.getId(), 1L);

        assertThat(film.getLikes()).isEmpty();
    }

    @Test
    void findByIdReturnsEmptyForUnknownFilm() {
        assertThat(filmStorage.findById(999L)).isEmpty();
    }

    private Film makeFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(90);
        film.setMpa(new Mpa(1, "G"));
        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        genres.add(new Genre(1, "Комедия"));
        film.setGenres(genres);
        return film;
    }
}
