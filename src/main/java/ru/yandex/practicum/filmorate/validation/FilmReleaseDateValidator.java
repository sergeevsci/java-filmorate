package ru.yandex.practicum.filmorate.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class FilmReleaseDateValidator implements ConstraintValidator<AfterCinemaBirthDate, LocalDate> {

    // Дата вынесена в константу
    private static final LocalDate CINEMA_BIRTH_DATE = LocalDate.of(1895, 12, 28);

    @Override
    public boolean isValid(LocalDate releaseDate, ConstraintValidatorContext context) {
        // Даты может не быть, отловит @NotNull
        if (releaseDate == null) {
            return true;
        }
        // Проверяем, что дата релиза не раньше CINEMA_BIRTH_DATE
        return !releaseDate.isBefore(CINEMA_BIRTH_DATE);
    }
}
