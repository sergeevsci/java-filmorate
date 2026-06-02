package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.Duration;

/**
 * Film.
 */

@Data
@EqualsAndHashCode(of = "id")
public class Film {
    private Long id;
    private String name;
    private String description;
    private LocalDate releaseDate;

    // Говорим Jackson превращать Duration в простое целое число - так как Postman ожидает увидеть число
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    private Duration duration;
}
