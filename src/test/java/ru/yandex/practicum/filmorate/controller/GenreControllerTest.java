package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.GenreService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GenreControllerTest {
    @Mock
    private GenreService genreService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GenreController(genreService)).build();
    }

    @Test
    void findAllReturnsGenres() throws Exception {
        when(genreService.findAll()).thenReturn(List.of(new Genre(1, "Комедия"), new Genre(2, "Драма")));

        mockMvc.perform(get("/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Комедия"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Драма"));

        verify(genreService).findAll();
    }

    @Test
    void findByIdReturnsGenre() throws Exception {
        when(genreService.findById(1)).thenReturn(new Genre(1, "Комедия"));

        mockMvc.perform(get("/genres/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Комедия"));

        verify(genreService).findById(1);
    }

    @Test
    void findByIdReturnsNotFoundForUnknownGenre() throws Exception {
        when(genreService.findById(999)).thenThrow(new NotFoundException("Жанр с ID 999 не найден"));

        mockMvc.perform(get("/genres/999"))
                .andExpect(status().isNotFound());

        verify(genreService).findById(999);
    }
}
