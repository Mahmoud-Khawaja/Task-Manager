package com.manager.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manager.taskmanager.dto.UserRequestDTO;
import com.manager.taskmanager.dto.UserResponseDTO;
import com.manager.taskmanager.exception.TaskNotFoundException;
import com.manager.taskmanager.model.Role;
import com.manager.taskmanager.model.User;
import com.manager.taskmanager.repository.UserRepository;
import com.manager.taskmanager.security.JwtUtil;
import com.manager.taskmanager.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    private UserRequestDTO userRequestDTO;
    private UserResponseDTO userResponseDTO;
    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.USER)
                .build();

        adminUser = User.builder()
                .id(99L)
                .username("admin")
                .email("admin@example.com")
                .role(Role.ADMIN)
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
    @DisplayName("POST /api/users - Admin should create user successfully")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createUser_Success() throws Exception {
        when(userService.createUser(any(UserRequestDTO.class)))
                .thenReturn(userResponseDTO);

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@example.com")));

        verify(userService, times(1)).createUser(any(UserRequestDTO.class));
    }

    @Test
    @DisplayName("GET /api/users - Admin should get all users")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAllUsers_Success() throws Exception {
        UserResponseDTO user2 = UserResponseDTO.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .build();

        List<UserResponseDTO> users = Arrays.asList(userResponseDTO, user2);

        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].username", is("testuser")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].username", is("user2")));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("GET /api/users/{id} - User should get own profile")
    @WithMockUser(username = "testuser", roles = "USER")
    void getUserById_OwnProfile() throws Exception {
        Long userId = 1L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(userService.getUserById(userId))
                .thenReturn(userResponseDTO);

        mockMvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")));

        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return 403 when user tries to view another profile")
    @WithMockUser(username = "testuser", roles = "USER")
    void getUserById_Forbidden() throws Exception {
        Long otherUserId = 2L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/users/{id}", otherUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("You can only view your own profile!"));

        verify(userService, never()).getUserById(any());
    }

    @Test
    @DisplayName("GET /api/users/{id} - Admin should access any profile")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getUserById_Admin() throws Exception {
        Long userId = 1L;

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        when(userService.getUserById(userId))
                .thenReturn(userResponseDTO);

        mockMvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));

        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return 404 when user not found")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getUserById_NotFound() throws Exception {
        Long userId = 999L;

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        when(userService.getUserById(userId))
                .thenThrow(new TaskNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - User should update own profile")
    @WithMockUser(username = "testuser", roles = "USER")
    void updateUser_OwnProfile() throws Exception {
        Long userId = 1L;

        UserResponseDTO updatedUser = UserResponseDTO.builder()
                .id(1L)
                .username("updateduser")
                .email("updated@example.com")
                .build();

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(userService.updateUser(eq(userId), any(UserRequestDTO.class)))
                .thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("updateduser")));

        verify(userService, times(1)).updateUser(eq(userId), any(UserRequestDTO.class));
    }

    @Test
    @DisplayName("PUT /api/users/{id} - Should return 403 when user tries to update another profile")
    @WithMockUser(username = "testuser", roles = "USER")
    void updateUser_Forbidden() throws Exception {
        Long otherUserId = 2L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        mockMvc.perform(put("/api/users/{id}", otherUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDTO)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("You can only update your own profile!"));

        verify(userService, never()).updateUser(any(), any());
    }

    @Test
    @DisplayName("PUT /api/users/{id} - Admin should update any profile")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updateUser_Admin() throws Exception {
        Long userId = 1L;

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        when(userService.updateUser(eq(userId), any(UserRequestDTO.class)))
                .thenReturn(userResponseDTO);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDTO)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(userService, times(1)).updateUser(eq(userId), any(UserRequestDTO.class));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Admin should delete user")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deleteUser_Success() throws Exception {
        Long userId = 1L;

        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(userId);
    }
}