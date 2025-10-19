package com.bookingsystem.service;

import com.bookingsystem.api.dto.UserCreateDto;
import com.bookingsystem.api.dto.UserUpdateDto;
import com.bookingsystem.exceptions.UserNotFoundException;
import com.bookingsystem.model.User;
import com.bookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.bookingsystem.model.EntityType.USER;
import static com.bookingsystem.model.EventOperation.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EventService eventService;

    @Transactional
    public User createUser(UserCreateDto dto) {
        val newUser = new User(
                dto.username(),
                dto.email()
        );
        val saved = userRepository.save(newUser);

        eventService.createEvent(
                USER,
                CREATE,
                saved.getId(),
                String.format("User created: %s (%s)", saved.getUsername(), saved.getEmail())
        );
        return saved;
    }

    @Transactional
    public User updateUser(Long id, UserUpdateDto dto) {
        return userRepository.findById(id)
                .map(user -> user.update(dto.username(), dto.email()))
                .map(userRepository::save)
                .map(user -> {
                    eventService.createEvent(
                            USER,
                            UPDATE,
                            user.getId(),
                            String.format("User updated: %s (%s)", user.getUsername(), user.getEmail())
                    );
                    return user;
                })
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        eventService.createEvent(
                USER,
                DELETE,
                id,
                String.format("User deleted: %s", id)
        );
    }
}
