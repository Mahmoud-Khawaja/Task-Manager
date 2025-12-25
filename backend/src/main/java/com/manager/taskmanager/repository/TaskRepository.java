package com.manager.taskmanager.repository;

import com.manager.taskmanager.model.Task;
import com.manager.taskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(com.manager.taskmanager.model.Status status);
    List<Task> findByUser(User user);
}
