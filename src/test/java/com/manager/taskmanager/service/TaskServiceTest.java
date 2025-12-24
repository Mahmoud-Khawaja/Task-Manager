package com.manager.taskmanager.service;

import com.manager.taskmanager.dto.TaskRequestDTO;
import com.manager.taskmanager.dto.TaskResponseDTO;
import com.manager.taskmanager.dto.UserResponseDTO;
import com.manager.taskmanager.exception.TaskNotFoundException;
import com.manager.taskmanager.model.Status;
import com.manager.taskmanager.model.Task;
import com.manager.taskmanager.model.User;
import com.manager.taskmanager.repository.TaskRepository;
import com.manager.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private Task testTask;
    private TaskRequestDTO taskRequestDTO;
    private TaskResponseDTO taskResponseDTO;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Status.TODO)
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        taskRequestDTO = TaskRequestDTO.builder()
                .title("New Task")
                .description("New Description")
                .status(Status.TODO)
                .build();

        userResponseDTO = UserResponseDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        taskResponseDTO = TaskResponseDTO.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(Status.TODO)
                .user(userResponseDTO)
                .build();
    }

    @Test
    @DisplayName("createTask should create task successfully when user exists")
    void createTask_Success() {
        Long userId = 1L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(modelMapper.map(taskRequestDTO, Task.class))
                .thenReturn(testTask);
        when(taskRepository.save(any(Task.class)))
                .thenReturn(testTask);
        when(modelMapper.map(testTask, TaskResponseDTO.class))
                .thenReturn(taskResponseDTO);

        TaskResponseDTO result = taskService.createTask(userId, taskRequestDTO);

        assertNotNull(result);
        assertEquals("Test Task", result.getTitle());
        assertEquals("Test Description", result.getDescription());
        assertEquals(Status.TODO, result.getStatus());
        assertNotNull(result.getUser());
        assertEquals(1L, result.getUser().getId());  // Access nested user.id
        assertEquals("testuser", result.getUser().getUsername());

        verify(userRepository, times(1)).findById(userId);
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(modelMapper, times(1)).map(taskRequestDTO, Task.class);
        verify(modelMapper, times(1)).map(testTask, TaskResponseDTO.class);
    }

    @Test
    @DisplayName("createTask should throw TaskNotFoundException when user does not exist")
    void createTask_UserNotFound() {
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(
                TaskNotFoundException.class,
                () -> taskService.createTask(userId, taskRequestDTO)
        );

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository, times(1)).findById(userId);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("getAllTasks should return all tasks")
    void getAllTasks_Success() {
        Task task2 = Task.builder()
                .id(2L)
                .title("Task 2")
                .description("Description 2")
                .status(Status.IN_PROGRESS)
                .user(testUser)
                .build();

        List<Task> tasks = Arrays.asList(testTask, task2);

        UserResponseDTO user2DTO = UserResponseDTO.builder()
                .id(1L)
                .username("testuser")
                .build();

        TaskResponseDTO dto2 = TaskResponseDTO.builder()
                .id(2L)
                .title("Task 2")
                .user(user2DTO)
                .build();

        when(taskRepository.findAll()).thenReturn(tasks);
        when(modelMapper.map(testTask, TaskResponseDTO.class))
                .thenReturn(taskResponseDTO);
        when(modelMapper.map(task2, TaskResponseDTO.class))
                .thenReturn(dto2);

        List<TaskResponseDTO> result = taskService.getAllTasks();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNotNull(result.get(0).getUser());
        assertNotNull(result.get(1).getUser());

        verify(taskRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllTasks should return empty list when no tasks exist")
    void getAllTasks_EmptyList() {
        when(taskRepository.findAll()).thenReturn(Collections.emptyList());

        List<TaskResponseDTO> result = taskService.getAllTasks();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());

        verify(taskRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getTasksByUser should return user's tasks")
    void getTasksByUser_Success() {
        Long userId = 1L;
        List<Task> userTasks = Arrays.asList(testTask);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
        when(taskRepository.findByUser(testUser))
                .thenReturn(userTasks);
        when(modelMapper.map(any(Task.class), eq(TaskResponseDTO.class)))
                .thenReturn(taskResponseDTO);

        List<TaskResponseDTO> result = taskService.getTasksByUser(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getUser());
        assertEquals(1L, result.get(0).getUser().getId());

        verify(userRepository, times(1)).findById(userId);
        verify(taskRepository, times(1)).findByUser(testUser);
    }

    @Test
    @DisplayName("getTaskById should return task when ID exists")
    void getTaskById_Success() {
        Long taskId = 1L;

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(testTask));
        when(modelMapper.map(testTask, TaskResponseDTO.class))
                .thenReturn(taskResponseDTO);

        TaskResponseDTO result = taskService.getTaskById(taskId);

        assertNotNull(result);
        assertEquals(taskId, result.getId());
        assertEquals("Test Task", result.getTitle());
        assertNotNull(result.getUser());
        assertEquals(1L, result.getUser().getId());

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    @DisplayName("getTaskById should throw TaskNotFoundException when task not found")
    void getTaskById_NotFound() {
        Long taskId = 999L;

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskService.getTaskById(taskId)
        );

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    @DisplayName("getTaskOwnerId should return owner ID")
    void getTaskOwnerId_Success() {
        Long taskId = 1L;

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(testTask));

        Long ownerId = taskService.getTaskOwnerId(taskId);

        assertNotNull(ownerId);
        assertEquals(1L, ownerId);
        assertEquals(testUser.getId(), ownerId);

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    @DisplayName("getTaskOwnerId should throw TaskNotFoundException when task not found")
    void getTaskOwnerId_NotFound() {
        Long taskId = 999L;

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskService.getTaskOwnerId(taskId)
        );

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    @DisplayName("updateTask should update task successfully")
    void updateTask_Success() {
        Long taskId = 1L;

        TaskRequestDTO updateDTO = TaskRequestDTO.builder()
                .title("Updated Title")
                .description("Updated Description")
                .status(Status.DONE)
                .build();

        TaskResponseDTO updatedDTO = TaskResponseDTO.builder()
                .id(1L)
                .title("Updated Title")
                .user(userResponseDTO)
                .build();

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class)))
                .thenReturn(testTask);
        when(modelMapper.map(testTask, TaskResponseDTO.class))
                .thenReturn(updatedDTO);

        TaskResponseDTO result = taskService.updateTask(taskId, updateDTO);

        assertNotNull(result);
        assertNotNull(result.getUser());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("updateTask should throw TaskNotFoundException when task not found")
    void updateTask_NotFound() {
        Long taskId = 999L;

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskService.updateTask(taskId, taskRequestDTO)
        );

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteTask should delete task successfully")
    void deleteTask_Success() {
        Long taskId = 1L;

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.of(testTask));
        doNothing().when(taskRepository).delete(testTask);

        taskService.deleteTask(taskId);

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, times(1)).delete(testTask);
    }

    @Test
    @DisplayName("deleteTask should throw TaskNotFoundException when task not found")
    void deleteTask_NotFound() {
        Long taskId = 999L;

        when(taskRepository.findById(taskId))
                .thenReturn(Optional.empty());

        assertThrows(
                TaskNotFoundException.class,
                () -> taskService.deleteTask(taskId)
        );

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, never()).delete(any(Task.class));
    }
}