package ru.yandex.practicum.filmorate.storage;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository("filmDbStorage")
public class FilmDbStorage implements FilmStorage {
    private static final String FIND_ALL_QUERY = """
            SELECT f.*, m.name AS mpa_name
            FROM films f
            JOIN mpa_ratings m ON f.mpa_id = m.id
            """;
    private static final String FIND_BY_ID_QUERY = FIND_ALL_QUERY + " WHERE f.id = ?";
    private static final String INSERT_QUERY = """
            INSERT INTO films(name, description, release_date, duration, mpa_id)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_QUERY = """
            UPDATE films
            SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ?
            WHERE id = ?
            """;
    private static final String EXISTS_QUERY = "SELECT COUNT(*) FROM films WHERE id = ?";
    private static final String FIND_LIKE_IDS_QUERY = "SELECT user_id FROM film_likes WHERE film_id = ?";
    private static final String ADD_LIKE_QUERY = """
            MERGE INTO film_likes(film_id, user_id) KEY(film_id, user_id)
            VALUES (?, ?)
            """;
    private static final String DELETE_LIKE_QUERY = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
    private static final String FIND_GENRES_QUERY = """
            SELECT g.id, g.name
            FROM genres g
            JOIN film_genres fg ON g.id = fg.genre_id
            WHERE fg.film_id = ?
            ORDER BY g.id
            """;
    private static final String DELETE_GENRES_QUERY = "DELETE FROM film_genres WHERE film_id = ?";
    private static final String ADD_GENRE_QUERY = """
            MERGE INTO film_genres(film_id, genre_id) KEY(film_id, genre_id)
            VALUES (?, ?)
            """;

    private final JdbcTemplate jdbc;
    private final FilmRowMapper mapper;

    public FilmDbStorage(JdbcTemplate jdbc, FilmRowMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public Film save(Film film) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setInt(5, film.getMpa().id());
            return ps;
        }, keyHolder);
        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        saveGenres(film);
        return findById(film.getId()).orElseThrow();
    }

    @Override
    public Film update(Film newFilm) {
        Film oldFilm = findById(newFilm.getId()).orElseThrow();
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
        if (newFilm.getMpa() != null) {
            oldFilm.setMpa(newFilm.getMpa());
        }
        jdbc.update(UPDATE_QUERY, oldFilm.getName(), oldFilm.getDescription(), Date.valueOf(oldFilm.getReleaseDate()),
                oldFilm.getDuration(), oldFilm.getMpa().id(), oldFilm.getId());
        if (!newFilm.getGenres().isEmpty()) {
            oldFilm.setGenres(newFilm.getGenres());
            saveGenres(oldFilm);
        }
        return findById(oldFilm.getId()).orElseThrow();
    }

    @Override
    public boolean exists(Long id) {
        Long count = jdbc.queryForObject(EXISTS_QUERY, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public Collection<Film> findAll() {
        List<Film> films = jdbc.query(FIND_ALL_QUERY, mapper);
        films.forEach(this::loadRelations);
        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        try {
            Film film = jdbc.queryForObject(FIND_BY_ID_QUERY, mapper, id);
            loadRelations(film);
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbc.update(ADD_LIKE_QUERY, filmId, userId);
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        jdbc.update(DELETE_LIKE_QUERY, filmId, userId);
    }

    private void loadRelations(Film film) {
        film.getLikes().addAll(jdbc.queryForList(FIND_LIKE_IDS_QUERY, Long.class, film.getId()));
        film.getGenres().addAll(jdbc.query(FIND_GENRES_QUERY,
                (rs, rowNum) -> new Genre(rs.getInt("id"), rs.getString("name")), film.getId()));
    }

    private void saveGenres(Film film) {
        jdbc.update(DELETE_GENRES_QUERY, film.getId());
        for (Genre genre : film.getGenres()) {
            jdbc.update(ADD_GENRE_QUERY, film.getId(), genre.id());
        }
    }
}
