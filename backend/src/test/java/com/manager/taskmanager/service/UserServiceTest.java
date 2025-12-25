package com.manager.taskmanager.service;

import com.manager.taskmanager.dto.UserRequestDTO;
import com.manager.taskmanager.dto.UserResponseDTO;
import com.manager.taskmanager.exception.UserNotFoundException;
import com.manager.taskmanager.exception.DuplicateResourceException;
import com.manager.taskmanager.model.Role;
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
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRequestDTO = UserRequestDTO.builder()
                .username("newuser")
                .email("newuser@example.com")
                .password("Password123")
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
        when(userRepository.findByUsername(userRequestDTO.getUsername()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(userRequestDTO.getEmail()))
                .thenReturn(Optional.empty());
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

        verify(userRepository, times(1)).findByUsername(userRequestDTO.getUsername());
        verify(userRepository, times(1)).findByEmail(userRequestDTO.getEmail());
        verify(passwordEncoder, times(1)).encode(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("createUser should throw DuplicateResourceException when username exists")
    void createUser_UsernameExists() {
        when(userRepository.findByUsername(userRequestDTO.getUsername()))
                .thenReturn(Optional.of(testUser));

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(userRequestDTO),
                "Username already exists"
        );

        verify(userRepository, times(1)).findByUsername(userRequestDTO.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("createUser should throw DuplicateResourceException when email exists")
    void createUser_EmailExists() {
        when(userRepository.findByUsername(userRequestDTO.getUsername()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(userRequestDTO.getEmail()))
                .thenReturn(Optional.of(testUser));

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(userRequestDTO),
                "Email already exists"
        );

        verify(userRepository, times(1)).findByUsername(userRequestDTO.getUsername());
        verify(userRepository, times(1)).findByEmail(userRequestDTO.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("getAllUsers should return all users")
    void getAllUsers_Success() {
        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .role(Role.USER)
                .build();

        List<User> users = Arrays.asList(testUser, user2);

        UserResponseDTO dto2 = UserResponseDTO.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
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
    @DisplayName("getUserById should throw UserNotFoundException when user not found")
    void getUserById_NotFound() {
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(userId)
        );

        assertEquals("User not found with id: 999", exception.getMessage());

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("updateUser should update all fields successfully")
    void updateUser_AllFields() {
        Long userId = 1L;

        UserRequestDTO updateDTO = UserRequestDTO.builder()
                .username("updateduser")
                .email("updated@example.com")
                .password("NewPassword123")
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("updateduser"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("updated@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("NewPassword123"))
                .thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(modelMapper.map(testUser, UserResponseDTO.class))
                .thenReturn(userResponseDTO);

        UserResponseDTO result = userService.updateUser(userId, updateDTO);

        assertNotNull(result);

        verify(userRepository, times(1)).findById(userId);
        verify(passwordEncoder, times(1)).encode("NewPassword123");
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
        when(userRepository.findByUsername("updateduser"))
                .thenReturn(Optional.empty());
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
                .password("NewPassword123")
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPassword123"))
                .thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);
        when(modelMapper.map(testUser, UserResponseDTO.class))
                .thenReturn(userResponseDTO);

        userService.updateUser(userId, updateDTO);

        verify(passwordEncoder, times(1)).encode("NewPassword123");
    }

    @Test
    @DisplayName("updateUser should throw DuplicateResourceException when username already exists")
    void updateUser_UsernameExists() {
        Long userId = 1L;

        User existingUser = User.builder()
                .id(2L)
                .username("existinguser")
                .email("existing@example.com")
                .build();

        UserRequestDTO updateDTO = UserRequestDTO.builder()
                .username("existinguser")
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("existinguser"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.updateUser(userId, updateDTO),
                "Username already exists"
        );

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser should throw DuplicateResourceException when email already exists")
    void updateUser_EmailExists() {
        Long userId = 1L;

        User existingUser = User.builder()
                .id(2L)
                .username("existinguser")
                .email("existing@example.com")
                .build();

        UserRequestDTO updateDTO = UserRequestDTO.builder()
                .email("existing@example.com")
                .build();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.updateUser(userId, updateDTO),
                "Email already exists"
        );

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser should throw UserNotFoundException when user not found")
    void updateUser_NotFound() {
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(userId, userRequestDTO)
        );

        assertEquals("User not found with id: 999", exception.getMessage());

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
    @DisplayName("deleteUser should throw UserNotFoundException when user not found")
    void deleteUser_NotFound() {
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(userId)
        );

        assertEquals("User not found with id: 999", exception.getMessage());

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).delete(any(User.class));
    }
}