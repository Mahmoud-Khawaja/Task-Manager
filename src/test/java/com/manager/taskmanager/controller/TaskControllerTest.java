package com.manager.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manager.taskmanager.dto.TaskRequestDTO;
import com.manager.taskmanager.dto.TaskResponseDTO;
import com.manager.taskmanager.dto.UserResponseDTO;
import com.manager.taskmanager.exception.TaskNotFoundException;
import com.manager.taskmanager.model.Role;
import com.manager.taskmanager.model.Status;
import com.manager.taskmanager.model.User;
import com.manager.taskmanager.repository.UserRepository;
import com.manager.taskmanager.security.JwtUtil;
import com.manager.taskmanager.service.TaskService;
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

@WebMvcTest(TaskController.class)
@DisplayName("TaskController Unit Tests")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserRepository userRepository;

    private TaskRequestDTO taskRequestDTO;
    private TaskResponseDTO taskResponseDTO;
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

        taskRequestDTO = TaskRequestDTO.builder()
                .title("New Task")
                .description("Task Description")
                .status(Status.TODO)
                .build();

        // Create UserResponseDTO for nested user object
        userResponseDTO = UserResponseDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        // TaskResponseDTO now has nested user object
        taskResponseDTO = TaskResponseDTO.builder()
                .id(1L)
                .title("New Task")
                .description("Task Description")
                .status(Status.TODO)
                .user(userResponseDTO)  // Nested user object
                .build();
    }

    @Test
    @DisplayName("POST /api/users/{userId}/tasks - Should create task successfully")
    @WithMockUser(username = "testuser", roles = "USER")
    void createTask_Success() throws Exception {
        Long userId = 1L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskService.createTask(eq(userId), any(TaskRequestDTO.class)))
                .thenReturn(taskResponseDTO);

        mockMvc.perform(post("/api/users/{userId}/tasks", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequestDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("New Task")))
                .andExpect(jsonPath("$.description", is("Task Description")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.user.id", is(1)))  // Access nested user.id
                .andExpect(jsonPath("$.user.username", is("testuser")))  // Can also check username
                .andExpect(jsonPath("$.user.email", is("test@example.com")));  // Can also check email

        verify(taskService, times(1)).createTask(eq(userId), any(TaskRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/users/{userId}/tasks - Should return 403 when user tries to create task for another user")
    @WithMockUser(username = "testuser", roles = "USER")
    void createTask_Forbidden() throws Exception {
        Long otherUserId = 2L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        mockMvc.perform(post("/api/users/{userId}/tasks", otherUserId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequestDTO)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("You can only create tasks for yourself!"));

        verify(taskService, never()).createTask(any(), any());
    }

    @Test
    @DisplayName("POST /api/users/{userId}/tasks - Admin should be able to create task for any user")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createTask_Admin() throws Exception {
        Long userId = 1L;

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        when(taskService.createTask(eq(userId), any(TaskRequestDTO.class)))
                .thenReturn(taskResponseDTO);

        mockMvc.perform(post("/api/users/{userId}/tasks", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequestDTO)))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(taskService, times(1)).createTask(eq(userId), any(TaskRequestDTO.class));
    }

    @Test
    @DisplayName("GET /api/tasks - Should return all tasks for admin")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAllTasks_Success() throws Exception {
        UserResponseDTO user2 = UserResponseDTO.builder()
                .id(2L)
                .username("user2")
                .build();

        TaskResponseDTO task2 = TaskResponseDTO.builder()
                .id(2L)
                .title("Task 2")
                .user(user2)
                .build();

        List<TaskResponseDTO> tasks = Arrays.asList(taskResponseDTO, task2);

        when(taskService.getAllTasks()).thenReturn(tasks);

        mockMvc.perform(get("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("New Task")))
                .andExpect(jsonPath("$[0].user.id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].title", is("Task 2")))
                .andExpect(jsonPath("$[1].user.id", is(2)));

        verify(taskService, times(1)).getAllTasks();
    }

    @Test
    @DisplayName("GET /api/users/{userId}/tasks - Should return user's tasks")
    @WithMockUser(username = "testuser", roles = "USER")
    void getTasksByUser_Success() throws Exception {
        Long userId = 1L;
        List<TaskResponseDTO> tasks = Arrays.asList(taskResponseDTO);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskService.getTasksByUser(userId))
                .thenReturn(tasks);

        mockMvc.perform(get("/api/users/{userId}/tasks", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].title", is("New Task")))
                .andExpect(jsonPath("$[0].user.id", is(1)));

        verify(taskService, times(1)).getTasksByUser(userId);
    }

    @Test
    @DisplayName("GET /api/users/{userId}/tasks - Should return 403 when user tries to view another user's tasks")
    @WithMockUser(username = "testuser", roles = "USER")
    void getTasksByUser_Forbidden() throws Exception {
        Long otherUserId = 2L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/users/{userId}/tasks", otherUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("You can only view your own tasks!"));

        verify(taskService, never()).getTasksByUser(any());
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - Should return task by ID")
    @WithMockUser(username = "testuser", roles = "USER")
    void getTaskById_Success() throws Exception {
        Long taskId = 1L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskService.getTaskOwnerId(taskId))
                .thenReturn(1L);
        when(taskService.getTaskById(taskId))
                .thenReturn(taskResponseDTO);

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("New Task")))
                .andExpect(jsonPath("$.user.id", is(1)));

        verify(taskService, times(1)).getTaskOwnerId(taskId);
        verify(taskService, times(1)).getTaskById(taskId);
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - Should return 404 when task not found")
    @WithMockUser(username = "testuser", roles = "USER")
    void getTaskById_NotFound() throws Exception {
        Long taskId = 999L;

        when(taskService.getTaskOwnerId(taskId))
                .thenThrow(new TaskNotFoundException("Task not found with id: " + taskId));

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - Should return 403 when user tries to view another user's task")
    @WithMockUser(username = "testuser", roles = "USER")
    void getTaskById_Forbidden() throws Exception {
        Long taskId = 1L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskService.getTaskOwnerId(taskId))
                .thenReturn(2L);  // Different user owns this task

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("You can only view your own tasks!"));

        verify(taskService, times(1)).getTaskOwnerId(taskId);
        verify(taskService, never()).getTaskById(any());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - Should update task successfully")
    @WithMockUser(username = "testuser", roles = "USER")
    void updateTask_Success() throws Exception {
        Long taskId = 1L;

        TaskRequestDTO updateDTO = TaskRequestDTO.builder()
                .title("Updated Task")
                .description("Updated Description")
                .status(Status.DONE)
                .build();

        TaskResponseDTO updatedResponse = TaskResponseDTO.builder()
                .id(1L)
                .title("Updated Task")
                .description("Updated Description")
                .status(Status.DONE)
                .user(userResponseDTO)
                .build();

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskService.getTaskOwnerId(taskId))
                .thenReturn(1L);
        when(taskService.updateTask(eq(taskId), any(TaskRequestDTO.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Task")))
                .andExpect(jsonPath("$.description", is("Updated Description")))
                .andExpect(jsonPath("$.status", is("DONE")))
                .andExpect(jsonPath("$.user.id", is(1)));

        verify(taskService, times(1)).updateTask(eq(taskId), any(TaskRequestDTO.class));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - Should return 403 when updating another user's task")
    @WithMockUser(username = "testuser", roles = "USER")
    void updateTask_Forbidden() throws Exception {
        Long taskId = 1L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskService.getTaskOwnerId(taskId))
                .thenReturn(2L);

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequestDTO)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("You can only update your own tasks!"));

        verify(taskService, never()).updateTask(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} - Should delete task successfully")
    @WithMockUser(username = "testuser", roles = "USER")
    void deleteTask_Success() throws Exception {
        Long taskId = 1L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskService.getTaskOwnerId(taskId))
                .thenReturn(1L);
        doNothing().when(taskService).deleteTask(taskId);

        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(taskId);
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} - Should return 403 when deleting another user's task")
    @WithMockUser(username = "testuser", roles = "USER")
    void deleteTask_Forbidden() throws Exception {
        Long taskId = 1L;

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));
        when(taskService.getTaskOwnerId(taskId))
                .thenReturn(2L);

        mockMvc.perform(delete("/api/tasks/{id}", taskId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().string("You can only delete your own tasks!"));

        verify(taskService, never()).deleteTask(any());
    }

    @Test
    @DisplayName("Admin should be able to access any task")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanAccessAnyTask() throws Exception {
        Long taskId = 1L;

        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(adminUser));
        when(taskService.getTaskOwnerId(taskId))
                .thenReturn(1L);
        when(taskService.getTaskById(taskId))
                .thenReturn(taskResponseDTO);

        mockMvc.perform(get("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.user.id", is(1)));
    }
}