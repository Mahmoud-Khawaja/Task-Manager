package com.manager.taskmanager.service;

import com.manager.taskmanager.dto.UserRequestDTO;
import com.manager.taskmanager.dto.UserResponseDTO;
import com.manager.taskmanager.exception.TaskNotFoundException;
import com.manager.taskmanager.model.User;
import com.manager.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRequestDTO userRequestDTO;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRequestDTO = UserRequestDTO.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("password123")
                .build();

        userResponseDTO = UserResponseDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Test
    @DisplayName("createUser should create user successfully")
    void createUser_Success() {
        when(modelMapper.map(userRequestDTO, User.class))
                .thenReturn(testUser);
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(modelMapper.map(testUser, UserResponseDTO.class))
                .thenReturn(userResponseDTO);

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        verify(passwordEncoder, times(1)).encode(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("getAllUsers should return all users")
    void getAllUsers_Success() {
        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .build();

        List<User> users = Arrays.asList(testUser, user2);

        UserResponseDTO dto2 = UserResponseDTO.builder()
                .id(2L)
                .username("user2")
                .build();

        when(userRepository.findAll()).thenReturn(users);
        when(modelMapper.map(testUser, UserResponseDTO.class))
                .thenReturn(userResponseDTO);
        when(modelMapper.map(user2, UserResponseDTO.class))
                .thenReturn(dto2);

        List<UserResponseDTO> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllUsers should return empty list when no users exist")
    void getAllUsers_EmptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<UserResponseDTO> result = userService.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getUserById should return user when ID exists")
    void getUserById_Success() {
        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(modelMapper.map(testUser, UserResponseDTO.class))
                .thenReturn(userResponseDTO);

        UserResponseDTO result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals("testuser", result.getUsername());

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserById should throw TaskNotFoundException when user not found")
    void getUserById_NotFound() {
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> userService.getUserById(userId)
        );

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("updateUser should update all fields successfully")
    void updateUser_AllFields() {
        Long userId = 1L;

        UserRequestDTO updateDTO = UserRequestDTO.builder()
                .username("updateduser")
                .email("updated@example.com")
                .password("newpassword")
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newpassword"))
                .thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(modelMapper.map(testUser, UserResponseDTO.class))
                .thenReturn(userResponseDTO);

        UserResponseDTO result = userService.updateUser(userId, updateDTO);

        assertNotNull(result);

        verify(userRepository, times(1)).findById(userId);
        verify(passwordEncoder, times(1)).encode("newpassword");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser should update only provided fields")
    void updateUser_PartialUpdate() {
        Long userId = 1L;

        UserRequestDTO updateDTO = UserRequestDTO.builder()
                .username("updateduser")
                .email(null)
                .password(null)
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(modelMapper.map(testUser, UserResponseDTO.class))
                .thenReturn(userResponseDTO);

        UserResponseDTO result = userService.updateUser(userId, updateDTO);

        assertNotNull(result);

        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("updateUser should encode password when provided")
    void updateUser_PasswordEncoding() {
        Long userId = 1L;

        UserRequestDTO updateDTO = UserRequestDTO.builder()
                .password("newPassword123")
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123"))
                .thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(modelMapper.map(testUser, UserResponseDTO.class))
                .thenReturn(userResponseDTO);

        userService.updateUser(userId, updateDTO);

        verify(passwordEncoder, times(1)).encode("newPassword123");
    }

    @Test
    @DisplayName("updateUser should throw TaskNotFoundException when user not found")
    void updateUser_NotFound() {
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> userService.updateUser(userId, userRequestDTO)
        );

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteUser should delete user successfully")
    void deleteUser_Success() {
        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        userService.deleteUser(userId);

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    @DisplayName("deleteUser should throw TaskNotFoundException when user not found")
    void deleteUser_NotFound() {
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> userService.deleteUser(userId)
        );

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }
}