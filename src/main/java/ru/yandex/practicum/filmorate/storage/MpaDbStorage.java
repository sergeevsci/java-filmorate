package ru.yandex.practicum.filmorate.storage;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.Collection;
import java.util.Optional;

@Repository
public class MpaDbStorage {
    private static final String FIND_ALL_QUERY = "SELECT id, name FROM mpa_ratings ORDER BY id";
    private static final String FIND_BY_ID_QUERY = "SELECT id, name FROM mpa_ratings WHERE id = ?";

    private final JdbcTemplate jdbc;

    public MpaDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Collection<Mpa> findAll() {
        return jdbc.query(FIND_ALL_QUERY,
                (rs, rowNum) -> new Mpa(rs.getInt("id"), rs.getString("name")));
    }

    public Optional<Mpa> findById(Integer id) {
        try {
            Mpa mpa = jdbc.queryForObject(FIND_BY_ID_QUERY,
                    (rs, rowNum) -> new Mpa(rs.getInt("id"), rs.getString("name")), id);
            return Optional.ofNullable(mpa);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }
}
