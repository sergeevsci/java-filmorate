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

import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

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
class UserControllerTest {

    private Validator validator;

    @Mock
    private InMemoryUserStorage userStorage;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userStorage)).build();
    }

    @Test
    void validUserPassesValidation() {
        User user = validUser();

        assertTrue(validator.validate(user).isEmpty());
    }

    @Test
    void rejectsBlankEmail() {
        User user = validUser();
        user.setEmail(" ");

        assertEquals(2, validator.validateProperty(user, "email").size());
    }

    @Test
    void rejectsNullEmail() {
        User user = validUser();
        user.setEmail(null);

        assertEquals(1, validator.validateProperty(user, "email").size());
    }

    @Test
    void rejectsInvalidEmail() {
        User user = validUser();
        user.setEmail("mail.example.com");

        assertEquals(1, validator.validateProperty(user, "email").size());
    }

    @Test
    void rejectsBlankLogin() {
        User user = validUser();
        user.setLogin("");

        assertEquals(1, validator.validateProperty(user, "login").size());
    }

    @Test
    void rejectsNullLogin() {
        User user = validUser();
        user.setLogin(null);

        assertEquals(1, validator.validateProperty(user, "login").size());
    }

    @Test
    void rejectsFutureBirthday() {
        User user = validUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        assertEquals(1, validator.validateProperty(user, "birthday").size());
    }

    @Test
    void acceptsBlankName() {
        User user = validUser();
        user.setName("  ");

        assertTrue(validator.validateProperty(user, "name").isEmpty());
    }

    @Test
    void createReplacesBlankNameWithLogin() {
        User user = validUser();
        user.setName(" ");
        when(userStorage.findAll()).thenReturn(Collections.emptyList());
        when(userStorage.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = new UserController(userStorage).create(user);

        assertEquals("login", created.getName());
        verify(userStorage).save(any(User.class));
    }

    @Test
    void createRejectsDuplicateLoginOrEmail() {
        User existing = validUser();
        existing.setId(1L);
        when(userStorage.findAll()).thenReturn(Collections.singletonList(existing));

        User duplicate = validUser();

        assertThrows(DuplicatedDataException.class, () -> new UserController(userStorage).create(duplicate));
        verify(userStorage, never()).save(any());
    }

    @Test
    void updateRejectsMissingId() {
        User user = validUser();
        when(userStorage.findAll()).thenReturn(Collections.emptyList());

        assertThrows(ConditionsNotMetException.class, () -> new UserController(userStorage).update(user));
        verify(userStorage, never()).update(any());
    }

    @Test
    void updateRejectsUnknownUser() {
        User user = validUser();
        user.setId(99L);

        assertThrows(NotFoundException.class, () -> new UserController(userStorage).update(user));
        verify(userStorage, never()).update(any());
    }

    @Test
    void createRejectsEmptyRequestBody() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    private User validUser() {
        User user = new User();
        user.setEmail("mail@example.com");
        user.setLogin("login");
        user.setName("name");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
