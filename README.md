# java-filmorate
Доброго дня, мой ревьюер по ER-диаграммке. Хорошей проверки!

## Прикрепляю диаграмму:
![ER-диаграмма базы данных](er-diagram-filmorate.png)

### Пояснение к схеме данных
* **Сущности:** `users` и `films`.
* **Справочники:** `mpa_ratings` и `genres`. Каждый фильм ссылается на один MPA (`Many-to-One`), но может иметь несколько жанров через связующую таблицу `film_genres` (`Many-to-Many`).
* **Связи:**
    * `film_likes` — фиксирует лайки пользователей к фильмам. Композитный ключ `(film_id, user_id)` исключает дублирование лайков.
    * `friendships` — рефлексивная связь между пользователями. Поля `user_id` (отправитель) и `friend_id` (получатель) ссылаются на таблицу `users`. Поле `status` принимает значения `UNCONFIRMED` или `CONFIRMED`.

---

## Примеры SQL-запросов для основных операций

### 1. Операции с фильмами (Films)

* **Получение всех фильмов (включая название их рейтинга MPA):**
```sql
SELECT f.*, m.name AS mpa_name
FROM films f
LEFT JOIN mpa_ratings m ON f.mpa_id = m.id;
```

* **Получение ТОП-N самых популярных фильмов по количеству лайков:**
```sql
SELECT f.*, COUNT(fl.user_id) AS rate
FROM films f
LEFT JOIN film_likes fl ON f.id = fl.film_id
GROUP BY f.id
ORDER BY rate DESC
LIMIT 10; -- 10 = N
```

* **Получение всех жанров для конкретного фильма (например, с id = 1):**
```sql
SELECT g.*
FROM genres g
JOIN film_genres fg ON g.id = fg.genre_id
WHERE fg.film_id = 1;
```

### 2. Операции с пользователями (Users)

* **Получение списка всех друзей пользователя (например, с id = 1):**
```sql
SELECT u.*
FROM users u
JOIN friendships f ON u.id = f.friend_id
WHERE f.user_id = 1 AND f.status = 'CONFIRMED';
```

* **Получение списка общих друзей между двумя пользователями (например, id = 1 и id = 2):**
```sql
SELECT * FROM users WHERE id IN (
    SELECT friend_id FROM friendships WHERE user_id = 1 AND status = 'CONFIRMED'
    INTERSECT
    SELECT friend_id FROM friendships WHERE user_id = 2 AND status = 'CONFIRMED'
);
```

### 3. Взаимодействия (Likes, Friends)

* **Добавление лайка фильму:**
```sql
INSERT INTO film_likes (film_id, user_id) 
VALUES (1, 5); -- Пользователь 5 лайкнул фильм 1
```

* **Отправка запроса в друзья:**
```sql
INSERT INTO friendships (user_id, friend_id, status) 
VALUES (1, 2, 'UNCONFIRMED'); -- Пользователь 1 отправил запрос пользователю 2
```

* **Подтверждение дружбы (принятие заявки):**
```sql
UPDATE friendships 
SET status = 'CONFIRMED' 
WHERE user_id = 1 AND friend_id = 2; -- Пользователь 2 подтвердил заявку от пользователя 1
```
