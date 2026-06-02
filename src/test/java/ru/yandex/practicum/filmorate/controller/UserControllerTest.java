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
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

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

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private InMemoryUserStorage userStorage; // используем Мок вместо запуска реального сервера

    private UserController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new UserController(userStorage);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createRejectsBlankEmail() {
        User user = validUser();
        user.setEmail(" ");

        assertThrows(ConditionsNotMetException.class, () -> controller.create(user));
        verify(userStorage, never()).save(any()); // удостоверяемся что никакие данные не ушли в хранилище
    }

    @Test
    void createRejectsNullEmail() {
        User user = validUser();
        user.setEmail(null);

        assertThrows(ConditionsNotMetException.class, () -> controller.create(user));
        verify(userStorage, never()).save(any());
    }

    @Test
    void createRejectsEmailWithoutAt() {
        User user = validUser();
        user.setEmail("mail.example.com");

        assertThrows(ConditionsNotMetException.class, () -> controller.create(user));
        verify(userStorage, never()).save(any());
    }

    @Test
    void createRejectsBlankLogin() {
        User user = validUser();
        user.setLogin("   ");

        assertThrows(ConditionsNotMetException.class, () -> controller.create(user));
        verify(userStorage, never()).save(any());
    }

    @Test
    void createRejectsNullLogin() {
        User user = validUser();
        user.setLogin(null);

        assertThrows(ConditionsNotMetException.class, () -> controller.create(user));
        verify(userStorage, never()).save(any());
    }

    @Test
    void createRejectsLoginWithSpaces() {
        User user = validUser();
        user.setLogin("bad login");

        assertThrows(ConditionsNotMetException.class, () -> controller.create(user));
        verify(userStorage, never()).save(any());
    }

    @Test
    void createRejectsFutureBirthday() {
        User user = validUser();
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ConditionsNotMetException.class, () -> controller.create(user));
        verify(userStorage, never()).save(any());
    }

    @Test
    void createRejectsDuplicateLoginOrEmail() {
        User existing = validUser();
        existing.setId(1L);
        when(userStorage.findAll()).thenReturn(Collections.singletonList(existing));

        User duplicate = validUser();

        assertThrows(DuplicatedDataException.class, () -> controller.create(duplicate));
        verify(userStorage, never()).save(any());
    }

    @Test
    void createReplacesBlankNameWithLogin() {
        User user = validUser();
        user.setName("  ");
        when(userStorage.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Когда вызовут метод save, не возвращай готовый объект, а посмотри, что тебе передали в параметрах, и верни это же самое
        // берёт самый первый аргумент (под индексом 0), который пришёл в метод save (то есть уже изменённого пользователя с подставленным именем), и возвращает его обратно.

        User created = controller.create(user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        // создаёт специальную «ловушку», настроенную на ловлю объектов класса User.


        verify(userStorage).save(captor.capture());
        // Во-первых, она проверяет, что метод save был вызван (как обычный verify).
        // Во-вторых, команда capture() перехватывает тот объект, который контроллер передал в этот метод, и сохраняет его внутри ловушки.

        assertEquals("login", captor.getValue().getName());
        // достаёт пойманный объект наружу, чтобы мы могли через assertEquals проверить его поля
        // (убедиться, что имя внутри объекта действительно изменилось на логин перед сохранением в базу).

        assertEquals("login", created.getName());
    }

    @Test
    void updateRejectsMissingId() {
        User user = validUser();

        assertThrows(ConditionsNotMetException.class, () -> controller.update(user));
        verify(userStorage, never()).update(any());
    }

    @Test
    void updateRejectsUnknownUser() {
        User user = validUser();
        user.setId(99L);

        assertThrows(NotFoundException.class, () -> controller.update(user));
        verify(userStorage, never()).update(any());
    }

    @Test
    void createRejectsEmptyRequestBody() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("")) // скормили пустой json
                .andExpect(status().isBadRequest()); // спринг обработал ошибку сам, вернул 400, мы это и ожидаем
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
