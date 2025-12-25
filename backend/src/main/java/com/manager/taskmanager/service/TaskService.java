package com.manager.taskmanager.service;

import com.manager.taskmanager.dto.TaskRequestDTO;
import com.manager.taskmanager.dto.TaskResponseDTO;
import com.manager.taskmanager.exception.TaskNotFoundException;
import com.manager.taskmanager.model.Task;
import com.manager.taskmanager.model.User;
import com.manager.taskmanager.repository.TaskRepository;
import com.manager.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    // Clean mapping using configured ModelMapper
    private TaskResponseDTO mapToDTO(Task task) {
        return modelMapper.map(task, TaskResponseDTO.class);
    }

    @Transactional
    public TaskResponseDTO createTask(Long userId, TaskRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TaskNotFoundException("User not found with id: " + userId));

        Task task = modelMapper.map(dto, Task.class);
        task.setUser(user);

        if (task.getStatus() == null) {
            task.setStatus(com.manager.taskmanager.model.Status.TODO);
        }

        Task savedTask = taskRepository.save(task);
        return mapToDTO(savedTask);
    }

    public List<TaskResponseDTO> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<TaskResponseDTO> getTasksByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TaskNotFoundException("User not found with id: " + userId));

        return taskRepository.findByUser(user)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public TaskResponseDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));
        return mapToDTO(task);
    }

    public Long getTaskOwnerId(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + taskId));
        return task.getUser().getId();
    }

    @Transactional
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));

        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getStatus() != null) task.setStatus(dto.getStatus());

        Task updatedTask = taskRepository.save(task);
        return mapToDTO(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));
        taskRepository.delete(task);
    }
}