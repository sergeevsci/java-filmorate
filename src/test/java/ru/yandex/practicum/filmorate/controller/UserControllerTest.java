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

import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
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
class UserControllerTest {

    private Validator validator;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
    }

    @Test
    void validUserPassesValidation() {
        User user = validUser();

        assertTrue(validator.validate(user, OnCreate.class).isEmpty());
    }

    @Test
    void rejectsBlankEmail() {
        User user = validUser();
        user.setEmail(" ");

        assertEquals(2, validator.validateProperty(user, "email", OnCreate.class).size());
    }

    @Test
    void rejectsNullEmail() {
        User user = validUser();
        user.setEmail(null);

        assertEquals(1, validator.validateProperty(user, "email", OnCreate.class).size());
    }

    @Test
    void rejectsInvalidEmail() {
        User user = validUser();
        user.setEmail("mail.example.com");

        assertEquals(1, validator.validateProperty(user, "email", OnCreate.class).size());
    }

    @Test
    void rejectsBlankLogin() {
        User user = validUser();
        user.setLogin("");

        assertEquals(1, validator.validateProperty(user, "login", OnCreate.class).size());
    }

    @Test
    void rejectsNullLogin() {
        User user = validUser();
        user.setLogin(null);

        assertEquals(1, validator.validateProperty(user, "login", OnCreate.class).size());
    }

    @Test
    void rejectsFutureBirthday() {
        User user = validUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        assertEquals(1, validator.validateProperty(user, "birthday", OnCreate.class).size());
    }

    @Test
    void acceptsBlankName() {
        User user = validUser();
        user.setName("  ");

        assertTrue(validator.validateProperty(user, "name", OnCreate.class).isEmpty());
    }

    @Test
    void createDelegatesToService() throws Exception {
        User user = validUser();
        user.setId(1L);
        when(userService.create(any(User.class))).thenReturn(user);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "mail@example.com",
                                  "login": "login",
                                  "name": "name",
                                  "birthday": "2000-01-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(userService).create(any(User.class));
    }

    @Test
    void updateRejectsMissingId() {
        User user = validUser();

        assertEquals(1, validator.validateProperty(user, "id", OnUpdate.class).size());
    }

    @Test
    void updateDelegatesToService() throws Exception {
        User user = validUser();
        user.setId(1L);
        when(userService.update(any(User.class))).thenReturn(user);

        mockMvc.perform(put("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 1,
                                  "email": "mail@example.com",
                                  "login": "login",
                                  "name": "name",
                                  "birthday": "2000-01-01"
                                }
                                """))
                .andExpect(status().isOk());

        verify(userService).update(any(User.class));
    }

    @Test
    void createRejectsEmptyRequestBody() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAllDelegatesToService() throws Exception {
        when(userService.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk());

        verify(userService).findAll();
    }

    @Test
    void addFriendDelegatesToService() throws Exception {
        mockMvc.perform(put("/users/1/friends/2"))
                .andExpect(status().isOk());

        verify(userService).addFriend(1L, 2L);
    }

    @Test
    void deleteFriendDelegatesToService() throws Exception {
        mockMvc.perform(delete("/users/1/friends/2"))
                .andExpect(status().isOk());

        verify(userService).deleteFriend(1L, 2L);
    }

    @Test
    void findFriendsDelegatesToService() throws Exception {
        when(userService.getFriends(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/users/1/friends"))
                .andExpect(status().isOk());

        verify(userService).getFriends(1L);
    }

    @Test
    void findCommonFriendsDelegatesToService() throws Exception {
        when(userService.getCommonFriends(1L, 2L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/users/1/friends/common/2"))
                .andExpect(status().isOk());

        verify(userService).getCommonFriends(1L, 2L);
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
