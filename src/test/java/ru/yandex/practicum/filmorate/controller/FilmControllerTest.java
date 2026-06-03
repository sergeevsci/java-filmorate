package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.validation.OnCreate; // ИМПОРТ ГРУППЫ
import ru.yandex.practicum.filmorate.validation.OnUpdate; // ИМПОРТ ГРУППЫ

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FilmControllerTest {

    private Validator validator;

    @Mock
    private FilmStorage filmStorage;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        mockMvc = MockMvcBuilders.standaloneSetup(new FilmController(filmStorage)).build();
    }

    @Test
    void validFilmPassesValidation() {
        Film film = validFilm();
        // Передаем OnCreate.class, чтобы валидатор знал, какой контекст проверять
        assertTrue(validator.validate(film, OnCreate.class).isEmpty());
    }

    @Test
    void rejectsBlankName() {
        Film film = validFilm();
        film.setName(" ");
        // Добавили OnCreate.class в параметры
        assertEquals(1, validator.validateProperty(film, "name", OnCreate.class).size());
    }

    @Test
    void rejectsNullName() {
        Film film = validFilm();
        film.setName(null);
        // Добавили OnCreate.class в параметры
        assertEquals(1, validator.validateProperty(film, "name", OnCreate.class).size());
    }

    @Test
    void rejectsTooLongDescription() {
        Film film = validFilm();
        film.setDescription("a".repeat(201));
        // Описание проверяется в обеих группах, укажем OnCreate.class
        assertEquals(1, validator.validateProperty(film, "description", OnCreate.class).size());
    }

    @Test
    void acceptsMaxDescriptionLength() {
        Film film = validFilm();
        film.setDescription("a".repeat(200));

        assertTrue(validator.validateProperty(film, "description", OnCreate.class).isEmpty());
    }

    @Test
    void rejectsNullReleaseDate() {
        Film film = validFilm();
        film.setReleaseDate(null);

        assertEquals(1, validator.validateProperty(film, "releaseDate", OnCreate.class).size());
    }

    @Test
    void createRejectsEmptyRequestBody() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsReleaseDateBeforeAllowedMinimum() {
        Film film = validFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertEquals(1, validator.validateProperty(film, "releaseDate", OnCreate.class).size());
    }

    @Test
    void rejectsZeroDuration() {
        Film film = validFilm();
        film.setDuration(0);

        assertEquals(1, validator.validateProperty(film, "duration", OnCreate.class).size());
    }

    @Test
    void createRejectsDuplicateFilmForSameYear() {
        Film existing = validFilm();
        existing.setId(1L);
        when(filmStorage.findAll()).thenReturn(Collections.singletonList(existing));

        Film duplicate = validFilm();

        assertThrows(DuplicatedDataException.class, () -> new FilmController(filmStorage).create(duplicate));
        verify(filmStorage, never()).save(any());
    }

    @Test
    void updateRejectsMissingId() {
        Film film = validFilm();
        // Теперь отсутствие ID при PUT запросе ловит сам Spring Validation через OnUpdate.class
        assertEquals(1, validator.validateProperty(film, "id", OnUpdate.class).size());
    }

    @Test
    void updateRejectsUnknownFilm() {
        Film film = validFilm();
        film.setId(77L);

        assertThrows(NotFoundException.class, () -> new FilmController(filmStorage).update(film));
        verify(filmStorage, never()).update(any());
    }

    @Test
    void acceptsReleaseDateBoundary() {
        Film film = validFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        assertTrue(validator.validateProperty(film, "releaseDate", OnCreate.class).isEmpty());
    }

    @Test
    void acceptsPositiveDurationBoundary() {
        Film film = validFilm();
        film.setDuration(1);

        assertTrue(validator.validateProperty(film, "duration", OnCreate.class).isEmpty());
    }

    private Film validFilm() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(90);
        return film;
    }
}
