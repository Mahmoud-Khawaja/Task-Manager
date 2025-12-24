package com.manager.taskmanager.controller;

import com.manager.taskmanager.dto.TaskRequestDTO;
import com.manager.taskmanager.dto.TaskResponseDTO;
import com.manager.taskmanager.model.User;
import com.manager.taskmanager.repository.UserRepository;
import com.manager.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    @PostMapping("/users/{userId}/tasks")
    public ResponseEntity<?> createTask(
            @PathVariable Long userId,
            @Valid @RequestBody TaskRequestDTO dto,
            Authentication authentication) {

        if (!isAuthorized(userId, authentication)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You can only create tasks for yourself!");
        }

        return new ResponseEntity<>(taskService.createTask(userId, dto), HttpStatus.CREATED);
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TaskResponseDTO>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/users/{userId}/tasks")
    public ResponseEntity<?> getTasksByUser(
            @PathVariable Long userId,
            Authentication authentication) {

        if (!isAuthorized(userId, authentication)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You can only view your own tasks!");
        }

        return ResponseEntity.ok(taskService.getTasksByUser(userId));
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<?> getTaskById(
            @PathVariable Long id,
            Authentication authentication) {

        Long taskUserId = taskService.getTaskOwnerId(id);

        if (!isAuthorized(taskUserId, authentication)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You can only view your own tasks!");
        }

        TaskResponseDTO task = taskService.getTaskById(id);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<?> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequestDTO dto,
            Authentication authentication) {

        Long taskUserId = taskService.getTaskOwnerId(id);

        if (!isAuthorized(taskUserId, authentication)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You can only update your own tasks!");
        }

        return ResponseEntity.ok(taskService.updateTask(id, dto));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<?> deleteTask(
            @PathVariable Long id,
            Authentication authentication) {

        Long taskUserId = taskService.getTaskOwnerId(id);

        if (!isAuthorized(taskUserId, authentication)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You can only delete your own tasks!");
        }

        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAuthorized(Long resourceUserId, Authentication authentication) {
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        String currentUsername = authentication.getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return currentUser.getId().equals(resourceUserId);
    }
}