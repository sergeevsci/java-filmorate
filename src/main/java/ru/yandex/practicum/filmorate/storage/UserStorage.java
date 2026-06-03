package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;
import java.util.Collection;

public interface UserStorage {
    User save(User user);
    User update(User user);
    boolean exists(Long id);
    Collection<User> findAll();
}
