package ru.yandex.practicum.filmorate.storage;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository("userDbStorage")
public class UserDbStorage implements UserStorage {
    private static final String FIND_ALL_QUERY = "SELECT * FROM users";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM users WHERE id = ?";
    private static final String INSERT_QUERY = """
            INSERT INTO users(email, login, name, birthday)
            VALUES (?, ?, ?, ?)
            """;
    private static final String UPDATE_QUERY = """
            UPDATE users
            SET email = ?, login = ?, name = ?, birthday = ?
            WHERE id = ?
            """;
    private static final String EXISTS_QUERY = "SELECT COUNT(*) FROM users WHERE id = ?";
    private static final String FIND_FRIEND_IDS_QUERY = """
            SELECT friend_id
            FROM friendships
            WHERE user_id = ?
            """;
    private static final String ADD_FRIEND_QUERY = """
            MERGE INTO friendships(user_id, friend_id) KEY(user_id, friend_id)
            VALUES (?, ?)
            """;
    private static final String DELETE_FRIEND_QUERY = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";

    private final JdbcTemplate jdbc;
    private final UserRowMapper mapper;

    public UserDbStorage(JdbcTemplate jdbc, UserRowMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);
        user.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return user;
    }

    @Override
    public User update(User newUser) {
        User oldUser = findById(newUser.getId()).orElseThrow();
        if (newUser.getEmail() != null) {
            oldUser.setEmail(newUser.getEmail());
        }
        if (newUser.getLogin() != null) {
            oldUser.setLogin(newUser.getLogin());
        }
        if (newUser.getName() != null) {
            oldUser.setName(newUser.getName());
        }
        if (newUser.getBirthday() != null) {
            oldUser.setBirthday(newUser.getBirthday());
        }
        jdbc.update(UPDATE_QUERY, oldUser.getEmail(), oldUser.getLogin(), oldUser.getName(),
                Date.valueOf(oldUser.getBirthday()), oldUser.getId());
        return oldUser;
    }

    @Override
    public boolean exists(Long id) {
        Long count = jdbc.queryForObject(EXISTS_QUERY, Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public Collection<User> findAll() {
        List<User> users = jdbc.query(FIND_ALL_QUERY, mapper);
        users.forEach(this::loadFriends);
        return users;
    }

    @Override
    public Optional<User> findById(Long id) {
        try {
            User user = jdbc.queryForObject(FIND_BY_ID_QUERY, mapper, id);
            loadFriends(user);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        jdbc.update(ADD_FRIEND_QUERY, userId, friendId);
    }

    @Override
    public void deleteFriend(Long userId, Long friendId) {
        jdbc.update(DELETE_FRIEND_QUERY, userId, friendId);
    }

    private void loadFriends(User user) {
        user.getFriends().addAll(jdbc.queryForList(FIND_FRIEND_IDS_QUERY, Long.class, user.getId()));
    }
}
