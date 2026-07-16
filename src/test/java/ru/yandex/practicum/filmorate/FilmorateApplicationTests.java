package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        UserDbStorage.class,
        FilmDbStorage.class,
        GenreDbStorage.class,
        MpaDbStorage.class,
        UserRowMapper.class,
        FilmRowMapper.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmorateApplicationTests {
    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;

    @Test
    void testFindUserById() {
        User savedUser = userStorage.save(makeUser("first"));

        Optional<User> userOptional = userStorage.findById(savedUser.getId());

        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(user -> assertThat(user)
                        .hasFieldOrPropertyWithValue("id", savedUser.getId())
                        .hasFieldOrPropertyWithValue("email", "first@example.com")
                        .hasFieldOrPropertyWithValue("login", "first"));
    }

    @Test
    void testFindUserByIdReturnsEmptyForUnknownId() {
        Optional<User> userOptional = userStorage.findById(999L);

        assertThat(userOptional).isEmpty();
    }

    @Test
    void testSaveUserGeneratesIdAndExists() {
        User savedUser = userStorage.save(makeUser("second"));

        assertThat(savedUser.getId()).isPositive();
        assertThat(userStorage.exists(savedUser.getId())).isTrue();
    }

    @Test
    void testUserExistsReturnsFalseForUnknownId() {
        assertThat(userStorage.exists(999L)).isFalse();
    }

    @Test
    void testUpdateUser() {
        User savedUser = userStorage.save(makeUser("third"));
        User update = new User();
        update.setId(savedUser.getId());
        update.setEmail("updated@example.com");
        update.setLogin("updated");
        update.setName("Updated Name");
        update.setBirthday(LocalDate.of(1999, 2, 3));

        User updatedUser = userStorage.update(update);

        assertThat(updatedUser)
                .hasFieldOrPropertyWithValue("id", savedUser.getId())
                .hasFieldOrPropertyWithValue("email", "updated@example.com")
                .hasFieldOrPropertyWithValue("login", "updated")
                .hasFieldOrPropertyWithValue("name", "Updated Name")
                .hasFieldOrPropertyWithValue("birthday", LocalDate.of(1999, 2, 3));
    }

    @Test
    void testFindAllUsers() {
        User firstUser = userStorage.save(makeUser("allfirst"));
        User secondUser = userStorage.save(makeUser("allsecond"));

        Collection<User> users = userStorage.findAll();

        assertThat(users)
                .extracting(User::getId)
                .contains(firstUser.getId(), secondUser.getId());
    }

    @Test
    void testAddAndDeleteFriendIsOneWay() {
        User user = userStorage.save(makeUser("user"));
        User friend = userStorage.save(makeUser("friend"));

        userStorage.addFriend(user.getId(), friend.getId());

        assertThat(userStorage.findById(user.getId()))
                .isPresent()
                .hasValueSatisfying(foundUser -> assertThat(foundUser.getFriends()).containsExactly(friend.getId()));
        assertThat(userStorage.findById(friend.getId()))
                .isPresent()
                .hasValueSatisfying(foundFriend -> assertThat(foundFriend.getFriends()).isEmpty());

        userStorage.deleteFriend(user.getId(), friend.getId());

        assertThat(userStorage.findById(user.getId()))
                .isPresent()
                .hasValueSatisfying(foundUser -> assertThat(foundUser.getFriends()).isEmpty());
    }

    @Test
    void testFindFilmById() {
        Film savedFilm = filmStorage.save(makeFilm("Film One"));

        Optional<Film> filmOptional = filmStorage.findById(savedFilm.getId());

        assertThat(filmOptional)
                .isPresent()
                .hasValueSatisfying(film -> assertThat(film)
                        .hasFieldOrPropertyWithValue("id", savedFilm.getId())
                        .hasFieldOrPropertyWithValue("name", "Film One")
                        .hasFieldOrPropertyWithValue("mpa", new Mpa(1, "G")));
    }

    @Test
    void testFindFilmByIdReturnsEmptyForUnknownId() {
        Optional<Film> filmOptional = filmStorage.findById(999L);

        assertThat(filmOptional).isEmpty();
    }

    @Test
    void testSaveFilmSavesGenres() {
        Film savedFilm = filmStorage.save(makeFilm("Film With Genres"));

        assertThat(savedFilm.getId()).isPositive();
        assertThat(filmStorage.exists(savedFilm.getId())).isTrue();
        assertThat(savedFilm.getGenres()).containsExactly(new Genre(1, "Комедия"), new Genre(2, "Драма"));
    }

    @Test
    void testFilmExistsReturnsFalseForUnknownId() {
        assertThat(filmStorage.exists(999L)).isFalse();
    }

    @Test
    void testUpdateFilm() {
        Film savedFilm = filmStorage.save(makeFilm("Old Film"));
        Film update = new Film();
        update.setId(savedFilm.getId());
        update.setName("New Film");
        update.setDescription("New description");
        update.setReleaseDate(LocalDate.of(2001, 4, 5));
        update.setDuration(120);
        update.setMpa(new Mpa(2, null));
        update.setGenres(new LinkedHashSet<>(Set.of(new Genre(3, null))));

        Film updatedFilm = filmStorage.update(update);

        assertThat(updatedFilm)
                .hasFieldOrPropertyWithValue("id", savedFilm.getId())
                .hasFieldOrPropertyWithValue("name", "New Film")
                .hasFieldOrPropertyWithValue("description", "New description")
                .hasFieldOrPropertyWithValue("releaseDate", LocalDate.of(2001, 4, 5))
                .hasFieldOrPropertyWithValue("duration", 120)
                .hasFieldOrPropertyWithValue("mpa", new Mpa(2, "PG"));
        assertThat(updatedFilm.getGenres()).containsExactly(new Genre(3, "Мультфильм"));
    }

    @Test
    void testFindAllFilms() {
        Film firstFilm = filmStorage.save(makeFilm("First Film"));
        Film secondFilm = filmStorage.save(makeFilm("Second Film"));

        Collection<Film> films = filmStorage.findAll();

        assertThat(films)
                .extracting(Film::getId)
                .contains(firstFilm.getId(), secondFilm.getId());
    }

    @Test
    void testAddAndDeleteLike() {
        User user = userStorage.save(makeUser("liker"));
        Film film = filmStorage.save(makeFilm("Liked Film"));

        filmStorage.addLike(film.getId(), user.getId());

        assertThat(filmStorage.findById(film.getId()))
                .isPresent()
                .hasValueSatisfying(foundFilm -> assertThat(foundFilm.getLikes()).containsExactly(user.getId()));

        filmStorage.deleteLike(film.getId(), user.getId());

        assertThat(filmStorage.findById(film.getId()))
                .isPresent()
                .hasValueSatisfying(foundFilm -> assertThat(foundFilm.getLikes()).isEmpty());
    }

    @Test
    void testFindAllGenres() {
        Collection<Genre> genres = genreStorage.findAll();

        assertThat(genres)
                .hasSize(6)
                .containsExactly(
                        new Genre(1, "Комедия"),
                        new Genre(2, "Драма"),
                        new Genre(3, "Мультфильм"),
                        new Genre(4, "Триллер"),
                        new Genre(5, "Документальный"),
                        new Genre(6, "Боевик")
                );
    }

    @Test
    void testFindGenreById() {
        Optional<Genre> genreOptional = genreStorage.findById(1);

        assertThat(genreOptional)
                .isPresent()
                .hasValue(new Genre(1, "Комедия"));
    }

    @Test
    void testFindGenreByIdReturnsEmptyForUnknownId() {
        Optional<Genre> genreOptional = genreStorage.findById(999);

        assertThat(genreOptional).isEmpty();
    }

    @Test
    void testFindAllMpaRatings() {
        Collection<Mpa> ratings = mpaStorage.findAll();

        assertThat(ratings)
                .hasSize(5)
                .containsExactly(new Mpa(1, "G"), new Mpa(2, "PG"), new Mpa(3, "PG-13"),
                        new Mpa(4, "R"), new Mpa(5, "NC-17"));
    }

    @Test
    void testFindMpaById() {
        Optional<Mpa> mpaOptional = mpaStorage.findById(1);

        assertThat(mpaOptional)
                .isPresent()
                .hasValue(new Mpa(1, "G"));
    }

    @Test
    void testFindMpaByIdReturnsEmptyForUnknownId() {
        Optional<Mpa> mpaOptional = mpaStorage.findById(999);

        assertThat(mpaOptional).isEmpty();
    }

    private User makeUser(String login) {
        User user = new User();
        user.setEmail(login + "@example.com");
        user.setLogin(login);
        user.setName("Name " + login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    private Film makeFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(90);
        film.setMpa(new Mpa(1, null));
        LinkedHashSet<Genre> genres = new LinkedHashSet<>();
        genres.add(new Genre(1, null));
        genres.add(new Genre(2, null));
        film.setGenres(genres);
        return film;
    }
}
