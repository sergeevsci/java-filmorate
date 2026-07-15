package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.MpaService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MpaControllerTest {
    @Mock
    private MpaService mpaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MpaController(mpaService)).build();
    }

    @Test
    void findAllReturnsMpaRatings() throws Exception {
        when(mpaService.findAll()).thenReturn(List.of(new Mpa(1, "G"), new Mpa(2, "PG"), new Mpa(3, "PG-13"),
                new Mpa(4, "R"), new Mpa(5, "NC-17")));

        mockMvc.perform(get("/mpa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("G"))
                .andExpect(jsonPath("$[4].id").value(5))
                .andExpect(jsonPath("$[4].name").value("NC-17"));

        verify(mpaService).findAll();
    }

    @Test
    void findByIdReturnsMpaRating() throws Exception {
        when(mpaService.findById(1)).thenReturn(new Mpa(1, "G"));

        mockMvc.perform(get("/mpa/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("G"));

        verify(mpaService).findById(1);
    }

    @Test
    void findByIdReturnsNotFoundForUnknownMpaRating() throws Exception {
        when(mpaService.findById(999)).thenThrow(new NotFoundException("Рейтинг MPA с ID 999 не найден"));

        mockMvc.perform(get("/mpa/999"))
                .andExpect(status().isNotFound());

        verify(mpaService).findById(999);
    }
}
