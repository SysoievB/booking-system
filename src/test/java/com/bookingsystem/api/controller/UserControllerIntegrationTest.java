package com.bookingsystem.api.controller;

import com.bookingsystem.api.dto.UserCreateDto;
import com.bookingsystem.api.dto.UserUpdateDto;
import com.bookingsystem.configuration.TestcontainersConfiguration;
import com.bookingsystem.repository.UserRepository;
import com.bookingsystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void should_create_user_with_valid_data() throws Exception {
        //given
        val createDto = new UserCreateDto("test", "testemail@example.com");

        //when & then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("test"))
                .andExpect(jsonPath("$.email").value("testemail@example.com"));
    }

    @Test
    void should_return_400_when_username_is_null() throws Exception {
        //given
        val invalidDto = new UserCreateDto(null, "john@example.com");

        //when & then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_email_is_null() throws Exception {
        //given
        val invalidDto = new UserCreateDto("john_doe", null);

        //when & then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_update_user_with_valid_data() throws Exception {
        //given
        val existingUser = userService.createUser(new UserCreateDto("test-update", "test-update@example.com"));
        val updateDto = new UserUpdateDto("updated", "updated@example.com");

        //when & then
        mockMvc.perform(put("/api/users/{id}", existingUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingUser.getId()))
                .andExpect(jsonPath("$.username").value("updated"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void should_return_404_when_user_not_found() throws Exception {
        //given
        val updateDto = new UserUpdateDto("updated", "updated@example.com");

        //when & then
        mockMvc.perform(put("/api/users/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void should_update_only_username() throws Exception {
        //given
        val existingUser = userService.createUser(new UserCreateDto("john", "john@example.com"));
        val updateDto = new UserUpdateDto("john_new", null);

        //when & then
        mockMvc.perform(put("/api/users/{id}", existingUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_new"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void should_return_user_when_found() throws Exception {
        //given
        val user = userService.createUser(new UserCreateDto("john", "john@example.com"));

        //when & then
        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void should_return_404_when_user_not_found_() throws Exception {
        //when & then
        mockMvc.perform(get("/api/users/{id}", 999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_empty_list_when_no_users() throws Exception {
        //when & then
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void should_return_all_users() throws Exception {
        //given
        userService.createUser(new UserCreateDto("user1", "user1@example.com"));
        userService.createUser(new UserCreateDto("user2", "user2@example.com"));
        userService.createUser(new UserCreateDto("user3", "user3@example.com"));

        //when & then
        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].username", containsInAnyOrder("user1", "user2", "user3")))
                .andExpect(jsonPath("$[*].email",
                        containsInAnyOrder(
                                "user1@example.com",
                                "user2@example.com",
                                "user3@example.com"
                        )));
    }

    @Test
    void should_delete_user_successfully() throws Exception {
        //given
        val user = userService.createUser(new UserCreateDto("john", "john@example.com"));

        //when & then
        mockMvc.perform(delete("/api/users/{id}", user.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    void should_return_404_when_user_not_found__() throws Exception {
        //when & then
        mockMvc.perform(delete("/api/users/{id}", 999L))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}