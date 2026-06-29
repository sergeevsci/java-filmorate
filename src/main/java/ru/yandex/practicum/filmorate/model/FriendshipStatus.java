package ru.yandex.practicum.filmorate.model;

import lombok.Getter;

@Getter
public enum FriendshipStatus {
    UNCONFIRMED("Неподтверждённая"),
    CONFIRMED("Подтверждённая");

    private final String name;

    FriendshipStatus(String name) {
        this.name = name;
    }
}
