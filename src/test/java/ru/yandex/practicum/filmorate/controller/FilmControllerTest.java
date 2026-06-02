package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class) // работает с фейковым сервером Мокито
class FilmControllerTest {

    @Mock // дубли внутри фейк-сервера
    private InMemoryFilmStorage filmStorage;

    private FilmController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() { // создает виртуальный симулятор
        controller = new FilmController(filmStorage);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createRejectsBlankName() {
        Film film = validFilm();
        film.setName(" ");

        assertThrows(ConditionsNotMetException.class, () -> controller.create(film));
        verify(filmStorage, never()).save(any());
    }

    @Test
    void createRejectsNullName() {
        Film film = validFilm();
        film.setName(null);

        assertThrows(ConditionsNotMetException.class, () -> controller.create(film));
        verify(filmStorage, never()).save(any());
    }

    @Test
    void createRejectsTooLongDescription() {
        Film film = validFilm();
        film.setDescription("a".repeat(201));

        assertThrows(ConditionsNotMetException.class, () -> controller.create(film));
        verify(filmStorage, never()).save(any());
    }

    @Test
    void createAcceptsMaxDescriptionLength() {
        Film film = validFilm();
        film.setDescription("a".repeat(200));
        when(filmStorage.save(any(Film.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Film created = controller.create(film);

        ArgumentCaptor<Film> captor = ArgumentCaptor.forClass(Film.class);
        verify(filmStorage).save(captor.capture());
        assertEquals(200, captor.getValue().getDescription().length());
        assertEquals("Film", created.getName());
    }

    @Test
    void createRejectsReleaseDateBeforeAllowedMinimum() {
        Film film = validFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ConditionsNotMetException.class, () -> controller.create(film));
        verify(filmStorage, never()).save(any());
    }

    @Test
    void createAcceptsReleaseDateBoundary() {
        Film film = validFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        when(filmStorage.save(any(Film.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Film created = controller.create(film);

        verify(filmStorage).save(any(Film.class));
        assertEquals(LocalDate.of(1895, 12, 28), created.getReleaseDate());
    }

    @Test
    void createRejectsInvalidDuration() {
        Film film = validFilm();
        film.setDuration(Duration.ZERO);

        assertThrows(ConditionsNotMetException.class, () -> controller.create(film));
        verify(filmStorage, never()).save(any());
    }

    @Test
    void createRejectsNullDuration() {
        Film film = validFilm();
        film.setDuration(null);

        assertThrows(ConditionsNotMetException.class, () -> controller.create(film));
        verify(filmStorage, never()).save(any());
    }

    @Test
    void createAcceptsPositiveDurationBoundary() {
        Film film = validFilm();
        film.setDuration(Duration.ofMinutes(1));
        when(filmStorage.save(any(Film.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Film created = controller.create(film);

        verify(filmStorage).save(any(Film.class));
        assertEquals(Duration.ofMinutes(1), created.getDuration());
    }

    @Test
    void createRejectsDuplicateFilmForSameYear() {
        Film existing = validFilm(); // создали существующий фильм
        existing.setId(1L); // вручную присвоили id = 1L, типа фильм сохранен
        when(filmStorage.findAll()).thenReturn(Collections.singletonList(existing));
        // Когда контроллер вызовет метод findAll() - верни список, в котором уже лежит наш фильм
        // Collections.singletonList — это просто быстрый способ создать список из одного элемента.

        Film duplicate = validFilm();

        assertThrows(DuplicatedDataException.class, () -> controller.create(duplicate));
        verify(filmStorage, never()).save(any()); // отловили ожидаемую ошибку
    }

    @Test
    void updateRejectsMissingId() {
        Film film = validFilm();

        assertThrows(ConditionsNotMetException.class, () -> controller.update(film));
        verify(filmStorage, never()).update(any());
    }

    @Test
    void updateRejectsUnknownFilm() {
        Film film = validFilm();
        film.setId(77L);

        assertThrows(NotFoundException.class, () -> controller.update(film));
        verify(filmStorage, never()).update(any());
    }

    @Test
    void createRejectsEmptyRequestBody() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    private Film validFilm() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(Duration.ofMinutes(90));
        return film;
    }
}
