package com.manager.taskmanager.service;

import com.manager.taskmanager.dto.UserRequestDTO;
import com.manager.taskmanager.dto.UserResponseDTO;
import com.manager.taskmanager.exception.TaskNotFoundException;
import com.manager.taskmanager.model.User;
import com.manager.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    private UserResponseDTO mapToDTO(User user) {
        return modelMapper.map(user, UserResponseDTO.class);
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {
        User user = modelMapper.map(dto, User.class);

        if (user.getPassword() != null) user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);
        return mapToDTO(savedUser);
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("User not found with id: " + id));
        return mapToDTO(user);
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("User not found with id: " + id));

        if (dto.getUsername() != null) user.setUsername(dto.getUsername());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());

        if (dto.getPassword() != null) user.setPassword(passwordEncoder.encode(dto.getPassword()));

        User updatedUser = userRepository.save(user);
        return mapToDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }
}