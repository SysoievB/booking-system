package com.bookingsystem.service;

import com.bookingsystem.api.dto.UserCreateDto;
import com.bookingsystem.api.dto.UserUpdateDto;
import com.bookingsystem.exceptions.UserNotFoundException;
import com.bookingsystem.repository.UserRepository;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    private static final Long USER_ID = 1L;
    private static final Long NON_EXISTENT_ID = 999L;
    private static final String USER_NAME = "john_doe";
    private static final String USER_EMAIL = "john@example.com";
    private static final String UPDATED_NAME = "jane_doe";
    private static final String UPDATED_EMAIL = "jane@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private UserService userService;

    @Test
    void create_user_should_save_user_and_create_event() {
        //given
        val user = EntitiesUtil.user().id(USER_ID).username(USER_NAME).email(USER_EMAIL).build();
        val dto = new UserCreateDto(USER_NAME, USER_EMAIL);

        given(userRepository.save(any())).willReturn(user);
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        //when
        val result = userService.createUser(dto);

        //then
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(USER_ID, result.getId());
            assertEquals(USER_NAME, result.getUsername());
            assertEquals(USER_EMAIL, result.getEmail());
        });
    }

    @Test
    void update_user_should_update_existing_user_and_create_event() {
        //given
        val existingUser = EntitiesUtil.user()
                .id(USER_ID).username(USER_NAME).email(USER_EMAIL)
                .build();
        val updatedUser = EntitiesUtil.user()
                .id(USER_ID).username(UPDATED_NAME).email(UPDATED_EMAIL)
                .build();
        val dto = new UserUpdateDto(UPDATED_NAME, UPDATED_EMAIL);

        given(userRepository.findById(any())).willReturn(Optional.of(existingUser));
        given(existingUser.update(any(), any())).willReturn(updatedUser);
        given(userRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        //when
        val result = userService.updateUser(USER_ID, dto);

        //then
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(USER_ID, result.getId());
            assertEquals(UPDATED_NAME, result.getUsername());
            assertEquals(UPDATED_EMAIL, result.getEmail());
        });
    }

    @Test
    void update_user_should_throw_exception_when_user_not_found() {
        //given
        val dto = new UserUpdateDto(UPDATED_NAME, UPDATED_EMAIL);
        given(userRepository.findById(any())).willReturn(Optional.empty());

        //when & then
        assertAll(() -> {
            assertThrows(
                    UserNotFoundException.class,
                    () -> userService.updateUser(NON_EXISTENT_ID, dto),
                    "User not found with id: 999"
            );
            verify(userRepository, never()).save(any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void get_user_by_id_should_return_user_when_exists() {
        //given
        val user = EntitiesUtil.user().id(USER_ID).username(USER_NAME).email(USER_EMAIL).build();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        //when
        val result = userService.getUserById(USER_ID);

        //then
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(USER_ID, result.getId());
            assertEquals(USER_NAME, result.getUsername());
            assertEquals(USER_EMAIL, result.getEmail());
            verifyNoInteractions(eventService);
        });
    }

    @Test
    void get_user_by_id_should_throw_exception_when_user_not_found() {
        //given
        given(userRepository.findById(NON_EXISTENT_ID)).willReturn(Optional.empty());

        //when & then
        assertAll(() -> {
            assertThrows(
                    UserNotFoundException.class,
                    () -> userService.getUserById(NON_EXISTENT_ID),
                    "User not found with id: 999"
            );
            verifyNoInteractions(eventService);
        });
    }

    @Test
    void delete_user_should_delete_existing_user_and_create_event() {
        //given
        given(userRepository.existsById(any())).willReturn(true);
        doNothing().when(userRepository).deleteById(USER_ID);
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        //when
        userService.deleteUser(USER_ID);

        //then
        assertAll(() -> {
            verify(userRepository).existsById(USER_ID);
            verify(userRepository).deleteById(USER_ID);
            verify(eventService).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void delete_user_should_throw_exception_when_user_not_found() {
        //given
        given(userRepository.existsById(any())).willReturn(false);

        //when & then
        assertAll(() -> {
            assertThrows(
                    UserNotFoundException.class,
                    () -> userService.deleteUser(NON_EXISTENT_ID),
                    "User not found with id: 999"
            );

            verify(userRepository).existsById(NON_EXISTENT_ID);
            verify(userRepository, never()).deleteById(any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }
}