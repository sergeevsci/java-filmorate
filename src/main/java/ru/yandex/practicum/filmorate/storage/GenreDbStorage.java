package ru.yandex.practicum.filmorate.storage;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.Optional;

@Repository
public class GenreDbStorage {
    private static final String FIND_ALL_QUERY = "SELECT id, name FROM genres ORDER BY id";
    private static final String FIND_BY_ID_QUERY = "SELECT id, name FROM genres WHERE id = ?";

    private final JdbcTemplate jdbc;

    public GenreDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Collection<Genre> findAll() {
        return jdbc.query(FIND_ALL_QUERY,
                (rs, rowNum) -> new Genre(rs.getInt("id"), rs.getString("name")));
    }

    public Optional<Genre> findById(Integer id) {
        try {
            Genre genre = jdbc.queryForObject(FIND_BY_ID_QUERY,
                    (rs, rowNum) -> new Genre(rs.getInt("id"), rs.getString("name")), id);
            return Optional.ofNullable(genre);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }
}
