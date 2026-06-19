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

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.validation.OnCreate;
import ru.yandex.practicum.filmorate.validation.OnUpdate;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FilmControllerTest {

    private Validator validator;

    @Mock
    private FilmService filmService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        mockMvc = MockMvcBuilders.standaloneSetup(new FilmController(filmService)).build();
    }

    @Test
    void validFilmPassesValidation() {
        Film film = validFilm();

        assertTrue(validator.validate(film, OnCreate.class).isEmpty());
    }

    @Test
    void rejectsBlankName() {
        Film film = validFilm();
        film.setName(" ");

        assertEquals(1, validator.validateProperty(film, "name", OnCreate.class).size());
    }

    @Test
    void rejectsNullName() {
        Film film = validFilm();
        film.setName(null);

        assertEquals(1, validator.validateProperty(film, "name", OnCreate.class).size());
    }

    @Test
    void rejectsTooLongDescription() {
        Film film = validFilm();
        film.setDescription("a".repeat(201));

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
    void createDelegatesToService() throws Exception {
        Film film = validFilm();
        film.setId(1L);
        when(filmService.create(any(Film.class))).thenReturn(film);

        // Переносим JSON на новую строку, чтобы скобки не слипались
        String jsonContent = "{\"name\":\"Film\",\"description\":\"Description\",\"releaseDate\":\"2000-01-01\",\"duration\":90}";


        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)) // Передаем готовую переменную
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(filmService).create(any(Film.class));
    }

    @Test
    void updateRejectsMissingId() {
        Film film = validFilm();

        assertEquals(1, validator.validateProperty(film, "id", OnUpdate.class).size());
    }

    @Test
    void updateDelegatesToService() throws Exception {
        Film film = validFilm();
        film.setId(1L);
        when(filmService.update(any(Film.class))).thenReturn(film);

        // Выносим текстовый блок в отдельную переменную
        String jsonContent = "{\"id\":1,\"name\":\"Film\",\"description\":\"Description\",\"releaseDate\":\"2000-01-01\",\"duration\":90}";


        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)) // Используем переменную
                .andExpect(status().isOk());

        verify(filmService).update(any(Film.class));
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

    @Test
    void findAllDelegatesToService() throws Exception {
        when(filmService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/films"))
                .andExpect(status().isOk());

        verify(filmService).findAll();
    }

    @Test
    void getPopularUsesDefaultCount() throws Exception {
        when(filmService.getPopularFilms(10)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/films/popular"))
                .andExpect(status().isOk());

        verify(filmService).getPopularFilms(10);
    }

    @Test
    void getPopularPassesCountToService() throws Exception {
        when(filmService.getPopularFilms(2)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/films/popular?count=2"))
                .andExpect(status().isOk());

        verify(filmService).getPopularFilms(2);
    }

    @Test
    void addLikeDelegatesToService() throws Exception {
        mockMvc.perform(put("/films/1/like/2"))
                .andExpect(status().isOk());

        verify(filmService).addLike(1L, 2L);
    }

    @Test
    void deleteLikeDelegatesToService() throws Exception {
        mockMvc.perform(delete("/films/1/like/2"))
                .andExpect(status().isOk());

        verify(filmService).deleteLike(1L, 2L);
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
