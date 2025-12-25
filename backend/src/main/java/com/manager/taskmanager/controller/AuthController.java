package com.manager.taskmanager.controller;

import com.manager.taskmanager.dto.AuthResponseDTO;
import com.manager.taskmanager.dto.LoginRequestDTO;
import com.manager.taskmanager.dto.RegisterRequestDTO;
import com.manager.taskmanager.model.Role;
import com.manager.taskmanager.model.User;
import com.manager.taskmanager.repository.UserRepository;
import com.manager.taskmanager.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponseDTO.builder()
                                .message("Username already exists!")
                                .build());
            }

            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(AuthResponseDTO.builder()
                                .message("Email already exists!")
                                .build());
            }

            String encodedPassword = passwordEncoder.encode(request.getPassword());

            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .role(request.getRole() != null ? request.getRole() : Role.USER)
                    .build();

            userRepository.save(user);

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            String token = jwtUtil.generateToken(userDetails, user.getRole().name());

            return ResponseEntity.ok(AuthResponseDTO.builder()
                    .token(token)
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .message("Registration successful!")
                    .build());

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponseDTO.builder()
                            .message("Registration failed: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails, user.getRole().name());

            return ResponseEntity.ok(AuthResponseDTO.builder()
                    .token(token)
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .message("Login successful!")
                    .build());

        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponseDTO.builder()
                            .message("Invalid username or password!")
                            .build());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponseDTO.builder()
                            .message("Login failed: " + e.getMessage())
                            .build());
        }
    }
}